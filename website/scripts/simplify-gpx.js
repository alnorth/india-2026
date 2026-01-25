#!/usr/bin/env node

/**
 * GPX Simplification Script
 *
 * Simplifies GPX files by:
 * 1. Removing timestamps (not used by the website)
 * 2. Reducing coordinate precision (5 decimal places = ~1.1m accuracy)
 * 3. Applying Douglas-Peucker line simplification algorithm
 */

const fs = require('fs')
const path = require('path')

// Configuration
const COORDINATE_PRECISION = 5  // 5 decimals = ~1.1m accuracy
const ELEVATION_PRECISION = 1   // 1 decimal for elevation
const TOLERANCE = 0.00005       // Douglas-Peucker tolerance (~5.5m)

/**
 * Parse GPX file and extract trackpoints
 */
function parseGPX(content) {
  const points = []
  const trkptRegex = /<trkpt\s+lat="([^"]+)"\s+lon="([^"]+)"[^>]*>([\s\S]*?)<\/trkpt>/g
  const eleRegex = /<ele>([^<]+)<\/ele>/

  let match
  while ((match = trkptRegex.exec(content)) !== null) {
    const lat = parseFloat(match[1])
    const lon = parseFloat(match[2])
    const eleMatch = eleRegex.exec(match[3])
    const ele = eleMatch ? parseFloat(eleMatch[1]) : null

    points.push({ lat, lon, ele })
  }

  return points
}

/**
 * Extract metadata from GPX (name, type)
 */
function extractMetadata(content) {
  const nameMatch = content.match(/<name>([^<]+)<\/name>/)
  const typeMatch = content.match(/<type>([^<]+)<\/type>/)

  return {
    name: nameMatch ? nameMatch[1] : null,
    type: typeMatch ? typeMatch[1] : null
  }
}

/**
 * Calculate perpendicular distance from point to line segment
 * Used for Douglas-Peucker algorithm
 */
function perpendicularDistance(point, lineStart, lineEnd) {
  const dx = lineEnd.lon - lineStart.lon
  const dy = lineEnd.lat - lineStart.lat

  if (dx === 0 && dy === 0) {
    // Line segment is a point
    const distX = point.lon - lineStart.lon
    const distY = point.lat - lineStart.lat
    return Math.sqrt(distX * distX + distY * distY)
  }

  const t = ((point.lon - lineStart.lon) * dx + (point.lat - lineStart.lat) * dy) / (dx * dx + dy * dy)
  const tClamped = Math.max(0, Math.min(1, t))

  const nearestX = lineStart.lon + tClamped * dx
  const nearestY = lineStart.lat + tClamped * dy

  const distX = point.lon - nearestX
  const distY = point.lat - nearestY

  return Math.sqrt(distX * distX + distY * distY)
}

/**
 * Douglas-Peucker line simplification algorithm
 */
function douglasPeucker(points, tolerance) {
  if (points.length <= 2) {
    return points
  }

  // Find point with maximum distance
  let maxDist = 0
  let maxIndex = 0

  const start = points[0]
  const end = points[points.length - 1]

  for (let i = 1; i < points.length - 1; i++) {
    const dist = perpendicularDistance(points[i], start, end)
    if (dist > maxDist) {
      maxDist = dist
      maxIndex = i
    }
  }

  // If max distance exceeds tolerance, recursively simplify
  if (maxDist > tolerance) {
    const left = douglasPeucker(points.slice(0, maxIndex + 1), tolerance)
    const right = douglasPeucker(points.slice(maxIndex), tolerance)

    // Concatenate results (removing duplicate point at junction)
    return left.slice(0, -1).concat(right)
  } else {
    // All intermediate points can be removed
    return [start, end]
  }
}

/**
 * Round number to specified precision
 */
function round(num, precision) {
  const factor = Math.pow(10, precision)
  return Math.round(num * factor) / factor
}

/**
 * Generate simplified GPX content
 */
function generateGPX(points, metadata) {
  let gpx = `<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
 <trk>
`

  if (metadata.name) {
    gpx += `  <name>${metadata.name}</name>\n`
  }
  if (metadata.type) {
    gpx += `  <type>${metadata.type}</type>\n`
  }

  gpx += `  <trkseg>\n`

  for (const point of points) {
    const lat = round(point.lat, COORDINATE_PRECISION)
    const lon = round(point.lon, COORDINATE_PRECISION)

    if (point.ele !== null) {
      const ele = round(point.ele, ELEVATION_PRECISION)
      gpx += `   <trkpt lat="${lat}" lon="${lon}"><ele>${ele}</ele></trkpt>\n`
    } else {
      gpx += `   <trkpt lat="${lat}" lon="${lon}"/>\n`
    }
  }

  gpx += `  </trkseg>
 </trk>
</gpx>
`

  return gpx
}

/**
 * Process a single GPX file
 */
function processFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8')
  const originalSize = Buffer.byteLength(content, 'utf-8')

  const points = parseGPX(content)
  const metadata = extractMetadata(content)

  const originalCount = points.length

  // Apply Douglas-Peucker simplification
  const simplified = douglasPeucker(points, TOLERANCE)

  const newContent = generateGPX(simplified, metadata)
  const newSize = Buffer.byteLength(newContent, 'utf-8')

  return {
    originalSize,
    newSize,
    originalCount,
    newCount: simplified.length,
    content: newContent
  }
}

/**
 * Format bytes as human readable string
 */
function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// Main
const daysDir = path.join(__dirname, '..', 'content', 'days')
const dayDirs = fs.readdirSync(daysDir).filter(d => d.startsWith('day-'))

let totalOriginalSize = 0
let totalNewSize = 0
let totalOriginalPoints = 0
let totalNewPoints = 0

console.log('Simplifying GPX files...\n')

for (const dayDir of dayDirs) {
  const gpxPath = path.join(daysDir, dayDir, 'route.gpx')

  if (!fs.existsSync(gpxPath)) {
    continue
  }

  const result = processFile(gpxPath)

  totalOriginalSize += result.originalSize
  totalNewSize += result.newSize
  totalOriginalPoints += result.originalCount
  totalNewPoints += result.newCount

  // Write simplified file
  fs.writeFileSync(gpxPath, result.content)

  const reduction = ((1 - result.newSize / result.originalSize) * 100).toFixed(1)
  console.log(`${dayDir}:`)
  console.log(`  Size: ${formatBytes(result.originalSize)} → ${formatBytes(result.newSize)} (${reduction}% reduction)`)
  console.log(`  Points: ${result.originalCount} → ${result.newCount}`)
  console.log()
}

const totalReduction = ((1 - totalNewSize / totalOriginalSize) * 100).toFixed(1)
console.log('='.repeat(50))
console.log('TOTAL:')
console.log(`  Size: ${formatBytes(totalOriginalSize)} → ${formatBytes(totalNewSize)} (${totalReduction}% reduction)`)
console.log(`  Points: ${totalOriginalPoints} → ${totalNewPoints}`)
