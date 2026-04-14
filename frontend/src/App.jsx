import { useEffect, useState } from 'react'

function App() {
    const [msg, setMsg] = useState('loading...')

    useEffect(() => {
        fetch('/api/ping')
            .then(r => r.text())
            .then(setMsg)
            .catch(e => setMsg('ERROR: ' + e.message))
    }, [])

    return (
        <div style={{ padding: 24 }}>
            <h1>EasyPark Frontend</h1>
            <p>Backend says: {msg}</p>
        </div>
    )
}

export default App
