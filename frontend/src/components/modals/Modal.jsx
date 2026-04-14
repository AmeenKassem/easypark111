import { useEffect } from 'react'

export default function Modal({ children, onClose }) {
    useEffect(() => {
        const onKeyDown = (e) => {
            if (e.key === 'Escape') onClose?.()
        }
        window.addEventListener('keydown', onKeyDown)
        return () => window.removeEventListener('keydown', onKeyDown)
    }, [onClose])

    return (
        <div
            onClick={onClose}
            style={{
                position: 'fixed',
                inset: 0,
                background: 'rgba(0,0,0,0.55)',
                zIndex: 10000,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: 16,
            }}
        >
            <div
                onClick={(e) => e.stopPropagation()}
                style={{
                    width: 'min(900px, 100%)',
                    maxHeight: '90vh',
                    overflow: 'auto',
                    background: '#fff',
                    borderRadius: 14,
                    boxShadow: '0 18px 50px rgba(0,0,0,0.35)',
                    position: 'relative',
                    padding: 16,
                }}
            >
                <button
                    type="button"
                    onClick={onClose}
                    style={{
                        position: 'absolute',
                        top: 10,
                        right: 10,
                        border: 0,
                        background: 'rgba(0,0,0,0.06)',
                        borderRadius: 10,
                        padding: '8px 10px',
                        cursor: 'pointer',
                        fontWeight: 800,
                    }}
                    aria-label="Close"
                    title="Close"
                >
                    ✕
                </button>

                {children}
            </div>
        </div>
    )
}
