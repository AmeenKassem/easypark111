// src/utils/timeOptions.js

export const generateTimeOptions = (stepMinutes = 15) => {
    const times = []
    for (let i = 0; i < 24 * 60; i += stepMinutes) {
        const hours = Math.floor(i / 60)
        const mins = i % 60
        times.push(`${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`)
    }
    return times
}
