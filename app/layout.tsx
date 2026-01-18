import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'India 2026 - Coast to Coast Cycle Tour',
  description: 'Following my coast to coast cycle tour across southern India',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>
        <header className="bg-blue-600 text-white py-6">
          <div className="container mx-auto px-4">
            <h1 className="text-3xl font-bold">India 2026</h1>
            <p className="text-blue-100">Coast to Coast Cycle Tour</p>
          </div>
        </header>
        <main className="container mx-auto px-4 py-8">
          {children}
        </main>
        <footer className="bg-gray-800 text-white py-6 mt-12">
          <div className="container mx-auto px-4 text-center">
            <p>&copy; 2026 India Cycle Tour</p>
          </div>
        </footer>
      </body>
    </html>
  )
}
