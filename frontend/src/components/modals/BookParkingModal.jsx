import React, { useState, useMemo, useEffect, useCallback, forwardRef } from 'react'
import { createBooking } from '../../services/booking'
import DatePicker from 'react-datepicker'
import 'react-datepicker/dist/react-datepicker.css'
import {API_BASE_URL} from "../../config.js";

// ---------- Helpers ----------
const addMinutesToTime = (timeStr, minutesToAdd) => {
    if (!timeStr) return '00:00'
    const mins = timeToMins(timeStr) + minutesToAdd
    return minsToTimeNoWrap(mins)
}

function timeToMins(t) {
    if (!t) return 0
    const [hStr, mStr] = String(t).split(':')
    const h = Number(hStr)
    const m = Number(mStr)
    if (Number.isNaN(h) || Number.isNaN(m)) return 0
    if (h === 24 && m === 0) return 1440
    return h * 60 + m
}

// Display 23:59 at the very end of the day
function minsToTimeNoWrap(mins) {
    const v = Math.max(0, Math.min(1440, Math.round(mins)))
    if (v >= 1440) return '23:59'

    const h = Math.floor(v / 60)
    const mm = v % 60
    return `${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
}

const toYMD = (d) => {
    if (!d) return ''
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
}

const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())

const isSameYMD = (a, b) => {
    if (!a || !b) return false
    return toYMD(a) === toYMD(b)
}

const findFirstSelectableDate = (baseDate, isSelectableDateFn, maxDays = 366) => {
    const d = startOfDay(baseDate)
    for (let i = 0; i < maxDays; i++) {
        if (isSelectableDateFn(d)) return new Date(d)
        d.setDate(d.getDate() + 1)
    }
    return null
}

const formatDuration = (ms) => {
    if (!Number.isFinite(ms) || ms <= 0) return ''
    const totalMin = Math.round(ms / 60000)
    const h = Math.floor(totalMin / 60)
    const m = totalMin % 60
    if (h <= 0) return `${m}m`
    if (m === 0) return `${h}h`
    return `${h}h ${m}m`
}

const dateAtMins = (dateObj, mins) => {
    const base = startOfDay(dateObj)
    return new Date(base.getTime() + mins * 60000)
}

// Slider helpers
const STEP_MIN = 15
const MAX_MIN = 24 * 60
const maxIndex = MAX_MIN / STEP_MIN // 96
const clamp = (v, min, max) => Math.max(min, Math.min(max, v))
const timeToIndex = (t) => clamp(Math.round(timeToMins(t) / STEP_MIN), 0, maxIndex)
const indexToTime = (idx) => minsToTimeNoWrap(idx * STEP_MIN)

// ---------- Styled DatePicker input ----------
const DateSelectLikeInput = forwardRef(function DateSelectLikeInput(props, ref) {
    const { value, onClick, placeholder = 'Select date' } = props
    return (
        <button
            type="button"
            ref={ref}
            onClick={onClick}
            style={{
                ...selectStyle,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                textAlign: 'left',
                backgroundImage: selectStyle.backgroundImage,
            }}
        >
            <span style={{ color: value ? '#0f172a' : '#94a3b8' }}>{value || placeholder}</span>
            <span style={{ width: 12, height: 12 }} />
        </button>
    )
})

// ---------- Dual-handle slider (single-day) ----------
function TimeRangeSlider({
                             startIdx,
                             endIdx,
                             onRequestStartIdx,
                             onRequestEndIdx,
                             overlayUnavailableAtIndex,
                             selectedDate,
                         }) {
    const [activeHandle, setActiveHandle] = useState('end')

    const safeStart = startIdx ?? 0
    const safeEnd = endIdx ?? 8

    const startPct = (safeStart / maxIndex) * 100
    const endPct = (safeEnd / maxIndex) * 100
    const trackLeft = Math.min(startPct, endPct)
    const trackWidth = Math.abs(endPct - startPct)

    // Merged segments for visual red areas
    const unavailableSegments = useMemo(() => {
        const segments = []
        let currentStart = null

        for (let i = 0; i < maxIndex; i++) {
            const isUnavailable = overlayUnavailableAtIndex(i)

            if (isUnavailable) {
                if (currentStart === null) currentStart = i
            } else {
                if (currentStart !== null) {
                    segments.push({ start: currentStart, length: i - currentStart })
                    currentStart = null
                }
            }
        }
        if (currentStart !== null) {
            segments.push({ start: currentStart, length: maxIndex - currentStart })
        }
        return segments
    }, [overlayUnavailableAtIndex])

    return (
        <div style={{ marginTop: 6 }}>
            <style>{globalCss}</style>

            {/* Summary row */}
            <div style={timeSummaryRow}>
                <div style={timePill}>
                    <span style={{ ...dotStyle, background: '#22c55e' }} />
                    <span style={timePillLabel}>Start</span>
                    <span style={timePillValue}>{indexToTime(safeStart)}</span>
                </div>

                <div style={timePill}>
                    <span style={{ ...dotStyle, background: '#0f172a' }} />
                    <span style={timePillLabel}>End</span>
                    <span style={timePillValue}>{indexToTime(safeEnd)}</span>
                </div>
            </div>

            {/* Duration */}
            <div style={{ marginTop: 8, textAlign: 'center' }}>
                <span style={durationPill}>
                    Duration:{' '}
                    <span style={{ fontWeight: 900, color: '#0f172a' }}>
                        {(() => {
                            if (!selectedDate) return '--'
                            const s = dateAtMins(selectedDate, safeStart * STEP_MIN)
                            // Treat maxIndex (96) as full 24h for calculation accuracy
                            let eM = safeEnd * STEP_MIN
                            return formatDuration(eM * 60000 - s.getTime() + startOfDay(selectedDate).getTime()) || '--'
                        })()}
                    </span>
                </span>
            </div>

            <div style={{ position: 'relative', marginTop: 10, height: 60 }}>
                {/* Rail */}
                <div
                    style={{
                        position: 'absolute',
                        left: 0,
                        right: 0,
                        top: 22,
                        height: 8,
                        background: '#e2e8f0',
                        borderRadius: 999,
                        overflow: 'hidden',
                        zIndex: 1,
                    }}
                >
                    {/* Unavailable overlay */}
                    <div style={{ position: 'absolute', inset: 0 }}>
                        {unavailableSegments.map((seg, i) => {
                            const leftPct = (seg.start / maxIndex) * 100
                            const widthPct = (seg.length / maxIndex) * 100
                            return (
                                <div
                                    key={i}
                                    style={{
                                        position: 'absolute',
                                        left: `${leftPct}%`,
                                        width: `calc(${widthPct}% + 1px)`,
                                        top: 0,
                                        bottom: 0,
                                        background: '#fecaca',
                                        opacity: 0.85,
                                    }}
                                />
                            )
                        })}
                    </div>

                    {/* Selected range */}
                    <div
                        style={{
                            position: 'absolute',
                            left: `${trackLeft}%`,
                            width: `${trackWidth}%`,
                            top: 0,
                            bottom: 0,
                            background: '#22c55e',
                            opacity: 0.9,
                        }}
                    />
                </div>

                {/* Labels under each handle */}
                <div
                    style={{
                        position: 'absolute',
                        left: 0,
                        right: 0,
                        top: 34,
                        height: 22,
                        zIndex: 2,
                        pointerEvents: 'none',
                    }}
                >
                    <div
                        style={{
                            position: 'absolute',
                            left: `calc(${startPct}% - 18px)`,
                            minWidth: 36,
                            textAlign: 'center',
                            fontSize: 11,
                            fontWeight: 800,
                            color: '#0f172a',
                            letterSpacing: 0.3,
                        }}
                    >
                        START
                    </div>
                    <div
                        style={{
                            position: 'absolute',
                            left: `calc(${endPct}% - 14px)`,
                            minWidth: 28,
                            textAlign: 'center',
                            fontSize: 11,
                            fontWeight: 800,
                            color: '#0f172a',
                            letterSpacing: 0.3,
                        }}
                    >
                        END
                    </div>
                </div>

                {/* Start handle */}
                <input
                    className="ep-range"
                    type="range"
                    min={0}
                    max={maxIndex - 1}
                    step={1}
                    value={safeStart}
                    onMouseDown={() => setActiveHandle('start')}
                    onTouchStart={() => setActiveHandle('start')}
                    onChange={(e) => onRequestStartIdx(Number(e.target.value))}
                    style={{ zIndex: activeHandle === 'start' ? 5 : 4, marginTop: 14,marginLeft: -2 }}
                    aria-label="Start time"
                />

                {/* End handle */}
                <input
                    className="ep-range"
                    type="range"
                    min={1}
                    max={maxIndex}
                    step={1}
                    value={safeEnd}
                    onMouseDown={() => setActiveHandle('end')}
                    onTouchStart={() => setActiveHandle('end')}
                    onChange={(e) => onRequestEndIdx(Number(e.target.value))}
                    style={{ zIndex: activeHandle === 'end' ? 5 : 3, marginTop: 14, marginLeft:4 }}
                    aria-label="End time"
                />
            </div>

            <div
                style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: 10,
                    color: '#94a3b8',
                    marginTop: -5,
                }}
            >
                <span>00:00</span>
                <span>06:00</span>
                <span>12:00</span>
                <span>18:00</span>
                <span>23:59</span>
            </div>
        </div>
    )
}
const ymdToLocalDate = (ymd) => {
    const [y, m, d] = String(ymd).split('-').map(Number);
    return new Date(y, m - 1, d); // local midnight
};
export default function BookParkingModal({ isOpen, onClose, spot, onBooked }) {
    if (!isOpen || !spot) return null

    // ---------- State ----------
    const [selectedDate, setSelectedDate] = useState(null)

    const [startTime, setStartTime] = useState('')
    const [endTime, setEndTime] = useState('')

    const [startIdx, setStartIdx] = useState(null)
    const [endIdx, setEndIdx] = useState(null)

    const [totalPrice, setTotalPrice] = useState(0)
    const [saving, setSaving] = useState(false)
    const [savingAdd, setSavingAdd] = useState(false)
    const [feedback, setFeedback] = useState({ message: '', isError: false })

    // New state for temporary alert
    const [tempAlert, setTempAlert] = useState(null)

    const [busyIntervals, setBusyIntervals] = useState([])
    const [busyLoading, setBusyLoading] = useState(false)

    // ---------- Availability normalization ----------
    const normalizeBackendDayToJs = (d) => {
        const n = Number(d);

        // Case A: backend already uses JS 0..6
        if (n >= 0 && n <= 6) return n;

        // Case B: backend uses Java DayOfWeek 1..7 (Mon..Sun)
        if (n >= 1 && n <= 7) return n === 7 ? 0 : n; // 7->0(Sun), 1->1(Mon), ... 6->6(Sat)

        // Unknown -> return null so it never matches
        return null;
    };

    const normalizedAvailabilityList = useMemo(() => {
        if (!spot) return [];
        const type = String(spot.availabilityType || '').toUpperCase();

        if (type === 'RECURRING' && Array.isArray(spot.recurringSchedule)) {
            return spot.recurringSchedule
                .map(r => ({
                    dayOfWeek: normalizeBackendDayToJs(r.dayOfWeek),
                    startTime: r.start,
                    endTime: r.end,
                }))
                .filter(r => r.dayOfWeek !== null);
        }

        if (type === 'SPECIFIC' && Array.isArray(spot.specificAvailability)) {
            return spot.specificAvailability.map(r => ({
                startDateTime: r.start,
                endDateTime: r.end,
            }));
        }

        return [];
    }, [spot]);


    const isSelectableDate = useCallback(
        (date) => {
            if (!date || !spot) return false
            const type = String(spot.availabilityType || '').trim().toUpperCase()

            if (!normalizedAvailabilityList || normalizedAvailabilityList.length === 0) return true

            if (type === 'RECURRING') {
                const jsDay = date.getDay(); // 0..6
                return normalizedAvailabilityList.some(r => r.dayOfWeek === jsDay);
            }


            if (type === 'SPECIFIC') {
                const targetYMD = toYMD(date)
                return normalizedAvailabilityList.some((r) => {
                    const sYMD = String(r.startDateTime || '').split('T')[0]
                    const eYMD = String(r.endDateTime || '').split('T')[0]
                    return targetYMD >= sYMD && targetYMD <= eYMD
                })
            }
            return true
        },
        [spot, normalizedAvailabilityList]
    )

    const getDailyLimits = useCallback(
        (dateStr) => {
            const type = String(spot.availabilityType || '').trim().toUpperCase()
            if (!dateStr || !spot) return { start: '00:00', end: '24:00' }

            if (!normalizedAvailabilityList || normalizedAvailabilityList.length === 0) {
                if (spot.startTime && spot.endTime) {
                    return { start: spot.startTime.substring(0, 5), end: spot.endTime.substring(0, 5) }
                }
                return { start: '00:00', end: '24:00' }
            }

            if (type === 'RECURRING') {
                const jsDay = ymdToLocalDate(dateStr).getDay(); // safer than new Date(dateStr)
                const rule = normalizedAvailabilityList.find(r => r.dayOfWeek === jsDay);
                if (!rule) return null;

                return {
                    start: String(rule.startTime).substring(0, 5),
                    end: String(rule.endTime).substring(0, 5),
                };
            }


            if (type === 'SPECIFIC') {
                const targetDate = new Date(dateStr + 'T00:00:00')

                const rule = normalizedAvailabilityList.find((r) => {
                    const startDT = new Date(r.startDateTime)
                    const endDT = new Date(r.endDateTime)
                    const startDay = startOfDay(startDT)
                    const endDay = startOfDay(endDT)
                    return targetDate >= startDay && targetDate <= endDay
                })

                if (rule) {
                    const s = new Date(rule.startDateTime)
                    const e = new Date(rule.endDateTime)

                    const startStr =
                        startOfDay(s).getTime() === targetDate.getTime()
                            ? `${String(s.getHours()).padStart(2, '0')}:${String(s.getMinutes()).padStart(2, '0')}`
                            : '00:00'
                    const endStr =
                        startOfDay(e).getTime() === targetDate.getTime()
                            ? `${String(e.getHours()).padStart(2, '0')}:${String(e.getMinutes()).padStart(2, '0')}`
                            : '24:00'

                    return { start: startStr, end: endStr }
                }

                return { start: '00:00', end: '24:00' }
            }

            return { start: '00:00', end: '24:00' }
        },
        [spot, normalizedAvailabilityList]
    )

    // ---------- Busy fetch ----------
    useEffect(() => {
        if (!isOpen || !spot?.id || !selectedDate) return
        const ymd = toYMD(selectedDate)

        let cancelled = false
        const fetchBusy = async () => {
            setBusyLoading(true)
            try {
                const fromStr = `${ymd}T00:00:00`
                const toStr = `${ymd}T23:59:59`

                const params = new URLSearchParams({ from: fromStr, to: toStr })
                const token = localStorage.getItem('easypark_token')

                const res = await fetch(`${API_BASE_URL}/api/parking-spots/${spot.id}/busy?${params}`, {
                    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
                })

                if (res.ok) {
                    const data = await res.json()
                    if (!cancelled) setBusyIntervals(Array.isArray(data) ? data : [])
                } else {
                    if (!cancelled) setBusyIntervals([])
                }
            } catch (err) {
                console.error('Failed to load busy intervals', err)
                if (!cancelled) setBusyIntervals([])
            } finally {
                if (!cancelled) setBusyLoading(false)
            }
        }

        fetchBusy()
        return () => {
            cancelled = true
        }
    }, [isOpen, spot, selectedDate])

    // ---------- Alert Timeout ----------
    useEffect(() => {
        if (tempAlert) {
            const timer = setTimeout(() => {
                setTempAlert(null)
            }, 3000)
            return () => clearTimeout(timer)
        }
    }, [tempAlert])

    // ---------- Validation helpers ----------
    const isOwnerClosed = useCallback(
        (dateObj, timeStr) => {
            const limits = getDailyLimits(toYMD(dateObj))
            const t = timeToMins(timeStr)
            return t < timeToMins(limits.start) || t > timeToMins(limits.end)
        },
        [getDailyLimits]
    )

    const isBookedBusyAtDateTime = useCallback(
        (dateObj, timeStr) => {
            const mins = timeToMins(timeStr)
            const fullDateTime = dateAtMins(dateObj, mins)
            return busyIntervals.some((interval) => {
                const busyStart = new Date(interval.startTime)
                const busyEnd = new Date(interval.endTime)
                return fullDateTime >= busyStart && fullDateTime < busyEnd
            })
        },
        [busyIntervals]
    )

    const hasOverlapForRange = useCallback(
        (startDT, endDT) => {
            return busyIntervals.some((interval) => {
                const bS = new Date(interval.startTime)
                const bE = new Date(interval.endTime)
                return startDT < bE && endDT > bS
            })
        },
        [busyIntervals]
    )

    const isInPastForStart = useCallback((dateObj, timeStr) => {
        if (!dateObj || !timeStr) return false
        const today = startOfDay(new Date())
        if (!isSameYMD(dateObj, today)) return false
        const now = new Date()
        const sDT = dateAtMins(dateObj, timeToMins(timeStr))
        return sDT < now
    }, [])

    const isRedAtIndex = useCallback(
        (idx) => {
            if (!selectedDate) return false
            const t = indexToTime(idx)
            if (isInPastForStart(selectedDate, t)) return true
            return isOwnerClosed(selectedDate, t) || isBookedBusyAtDateTime(selectedDate, t)
        },
        [selectedDate, isOwnerClosed, isBookedBusyAtDateTime, isInPastForStart]
    )

    const canApplyRangeNoRed = useCallback(
        (S, E) => {
            if (!selectedDate) return false
            const s = clamp(S, 0, maxIndex - 1)
            const e = clamp(E, 1, maxIndex)

            // Allow 15 min slots (1 step)
            if (e <= s) return false

            if (isRedAtIndex(s)) return false
            for (let i = s; i < e; i++) {
                if (isRedAtIndex(i)) return false
            }
            return true
        },
        [selectedDate, isRedAtIndex]
    )

    // FIX: Push Logic for Sliders (Drag & Push)
    // Relaxed checks to allow abutting ranges
    const moveHandleAllowJump = useCallback(
        ({ which, proposedIdx }) => {
            const curS = startIdx ?? 0
            const curE = endIdx ?? 1
            const currentDuration = Math.max(1, curE - curS)
            const minGap = 1

            let S = curS
            let E = curE

            if (which === 'start') {
                const nextS = clamp(proposedIdx, 0, maxIndex - minGap)
                // Relaxed: nextS < curE (allow 15 mins)
                if (nextS < curE && canApplyRangeNoRed(nextS, curE)) {
                    S = nextS
                }
                else {
                    const proposedEnd = clamp(nextS + currentDuration, nextS + minGap, maxIndex)
                    if (canApplyRangeNoRed(nextS, proposedEnd)) {
                        S = nextS
                        E = proposedEnd
                    } else {
                        const minEnd = clamp(nextS + minGap, nextS + minGap, maxIndex)
                        if (canApplyRangeNoRed(nextS, minEnd)) {
                            S = nextS
                            E = minEnd
                        }
                    }
                }
            } else {
                const nextE = clamp(proposedIdx, minGap, maxIndex)
                // Relaxed: nextE > curS (allow 15 mins)
                if (nextE > curS && canApplyRangeNoRed(curS, nextE)) {
                    E = nextE
                }
                else {
                    const proposedStart = clamp(nextE - currentDuration, 0, nextE - minGap)
                    if (canApplyRangeNoRed(proposedStart, nextE)) {
                        S = proposedStart
                        E = nextE
                    } else {
                        const minStart = clamp(nextE - minGap, 0, nextE - minGap)
                        if (canApplyRangeNoRed(minStart, nextE)) {
                            S = minStart
                            E = nextE
                        }
                    }
                }
            }

            return { S, E }
        },
        [startIdx, endIdx, canApplyRangeNoRed]
    )

    const applyRange = useCallback((S, E) => {
        const s = clamp(S, 0, maxIndex - 1)
        const e = clamp(E, 1, maxIndex)
        setStartIdx(s)
        setEndIdx(e)
        setStartTime(indexToTime(s))
        setEndTime(indexToTime(e))
    }, [])

    const onRequestStartIdx = (proposedIdx) => {
        const { S, E } = moveHandleAllowJump({ which: 'start', proposedIdx })
        applyRange(S, E)
        setFeedback({ message: '', isError: false })
    }

    const onRequestEndIdx = (proposedIdx) => {
        const { S, E } = moveHandleAllowJump({ which: 'end', proposedIdx })
        applyRange(S, E)
        setFeedback({ message: '', isError: false })
    }

    // ---------- Find first available range ----------
    const findFirstAvailableRange = useCallback(
        (dateObj, desiredMinutes = 120) => {
            const desiredSteps = Math.max(1, Math.round(desiredMinutes / STEP_MIN))

            for (let s = 0; s <= maxIndex - desiredSteps; s++) {
                const e = s + desiredSteps
                const isRedAtIndexForDate = (idx) => {
                    const t = indexToTime(idx)
                    if (isInPastForStart(dateObj, t)) return true
                    if (isOwnerClosed(dateObj, t)) return true
                    if (isBookedBusyAtDateTime(dateObj, t)) return true
                    return false
                }
                if (e <= s) continue
                if (isRedAtIndexForDate(s)) continue
                let ok = true
                for (let i = s; i < e; i++) {
                    if (isRedAtIndexForDate(i)) {
                        ok = false
                        break
                    }
                }
                if (ok) return { S: s, E: e }
            }
            const limits = getDailyLimits(toYMD(dateObj))
            const s0 = clamp(timeToIndex(limits.start || '00:00'), 0, maxIndex - 1)
            const e0 = clamp(s0 + desiredSteps, 1, maxIndex)
            return { S: s0, E: e0 }
        },
        [getDailyLimits, isInPastForStart, isOwnerClosed, isBookedBusyAtDateTime]
    )

    // ---------- Reset Logic ----------
    const resetSelection = useCallback(
        ({ dateOverride } = {}) => {
            const today = startOfDay(new Date())
            let base = today
            if (spot?.availableFrom) {
                const af = startOfDay(new Date(spot.availableFrom))
                if (af > base) base = af
            }
            let dateToUse = dateOverride || null
            if (!dateToUse) {
                const first = findFirstSelectableDate(base, isSelectableDate, 366)
                if (!first) {
                    setSelectedDate(null)
                    setStartTime('')
                    setEndTime('')
                    setStartIdx(null)
                    setEndIdx(null)
                    setFeedback({ message: 'No available dates for this spot.', isError: true })
                    return
                }
                dateToUse = first
            }
            setSelectedDate(dateToUse)
            const { S, E } = findFirstAvailableRange(dateToUse, 120)
            applyRange(S, E)
            setFeedback({ message: '', isError: false })
        },
        [spot, isSelectableDate, findFirstAvailableRange, applyRange]
    )

    useEffect(() => {
        if (!isOpen) return
        resetSelection()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen, spot, normalizedAvailabilityList])

    // Logic for updating range AND checking full day availability
    useEffect(() => {
        if (!isOpen) return
        if (!selectedDate) return

        const { S, E } = findFirstAvailableRange(selectedDate, 120)
        applyRange(S, E)
        setFeedback({ message: '', isError: false })

        // Ensure we check availability only when busy intervals are fully loaded
        if (!busyLoading) {
            const isValid = canApplyRangeNoRed(S, E)
            if (!isValid) {
                // Day is full - Trigger logic
                const day = String(selectedDate.getDate()).padStart(2, '0')
                const mon = String(selectedDate.getMonth() + 1).padStart(2, '0')
                setTempAlert(`${day}.${mon} has no time available`)

                // Jump to next day
                const nextDay = new Date(selectedDate)
                nextDay.setDate(nextDay.getDate() + 1)

                // Find next selectable date from the next day onwards
                const nextSelectable = findFirstSelectableDate(nextDay, isSelectableDate, 30)
                if (nextSelectable) {
                    setSelectedDate(nextSelectable)
                }
            }
        }
    }, [selectedDate, isOpen, findFirstAvailableRange, applyRange, busyLoading, canApplyRangeNoRed, isSelectableDate])

    // ---------- Price ----------
    useEffect(() => {
        if (!selectedDate || !startTime || !endTime) {
            setTotalPrice(0)
            return
        }

        const sM = timeToMins(startTime)
        let eM = timeToMins(endTime)

        // FIX: Treat '23:59' as 1440 minutes for pricing purposes
        if (endTime === '23:59') {
            eM = 1440
        }

        const s = dateAtMins(selectedDate, sM)
        const e = dateAtMins(selectedDate, eM)

        if (e > s) {
            const diffMs = e - s
            const diffHours = diffMs / (1000 * 60 * 60)
            setTotalPrice((diffHours * (spot.pricePerHour || 0)).toFixed(2))
        } else {
            setTotalPrice(0)
        }
    }, [selectedDate, startTime, endTime, spot])

    const getValidationError = () => {
        if (!selectedDate || !startTime || !endTime) return null

        const s = dateAtMins(selectedDate, timeToMins(startTime))
        const e = dateAtMins(selectedDate, timeToMins(endTime))

        if (e <= s) return 'End must be after start'
        if (isInPastForStart(selectedDate, startTime)) return 'Start time must be in the future.'
        if (isOwnerClosed(selectedDate, startTime) || isOwnerClosed(selectedDate, endTime))
            return 'Selected time is outside operating hours.'
        if (hasOverlapForRange(s, e)) return 'Selected range overlaps with an existing booking.'

        const sI = startIdx ?? timeToIndex(startTime)
        const eI = endIdx ?? timeToIndex(endTime)
        if (!canApplyRangeNoRed(sI, eI)) return 'Selected range includes unavailable time.'

        return null
    }

    const errorMsg = getValidationError()
    const overlayUnavailableAtIndex = useCallback((idx) => isRedAtIndex(idx), [isRedAtIndex])
    const popperModifiers = useMemo(() => [
        { name: 'offset', options: { offset: [0, 8] } },
        { name: 'preventOverflow', options: { boundary: 'viewport', padding: 8 } },
    ], [])

    const doCreateBooking = async () => {
        const sM = timeToMins(startTime)
        let eM = timeToMins(endTime)

        if (endTime === '23:59') {
            eM = 1440
        }

        const startDT = dateAtMins(selectedDate, sM)
        const endDT = dateAtMins(selectedDate, eM)

        const payload = {
            parkingId: spot.id,
            startTime: `${toYMD(startDT)}T${String(startDT.getHours()).padStart(2, '0')}:${String(
                startDT.getMinutes()
            ).padStart(2, '0')}:00`,

            endTime: `${toYMD(endDT)}T${String(endDT.getHours()).padStart(2, '0')}:${String(
                endDT.getMinutes()
            ).padStart(2, '0')}:00`,
        }

        await createBooking(payload)
    }

    const handleConfirm = async () => {
        if (errorMsg) return
        setSaving(true)
        setFeedback({ message: '', isError: false })
        try {
            await doCreateBooking()
            onBooked?.()
            onClose?.()
        } catch (e) {
            console.error(e)
            const msg = e.response?.data?.message || e.message || 'Booking failed. Try again.'
            setFeedback({ message: msg, isError: true })
        } finally {
            setSaving(false)
        }
    }

    const handleConfirmAndAddAnother = async () => {
        if (errorMsg) return
        setSavingAdd(true)
        setFeedback({ message: '', isError: false })
        try {
            await doCreateBooking()
            onBooked?.()
            setBusyIntervals([])
            resetSelection({ dateOverride: startOfDay(new Date()) })
        } catch (e) {
            console.error(e)
            const msg = e.response?.data?.message || e.message || 'Booking failed. Try again.'
            setFeedback({ message: msg, isError: true })
        } finally {
            setSavingAdd(false)
        }
    }

    return (
        <div style={overlayStyle}>
            <button
                onClick={onClose}
                style={{ position: 'absolute', inset: 0, background: 'rgba(15,23,42,0.6)', border: 0 }}
            />

            <div style={modalStyle}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '14px' }}>
                    <h2 style={{ margin: 0, fontSize: '20px', color: '#0f172a' }}>Request Booking</h2>
                    <button onClick={onClose} style={closeBtnStyle}>
                        &times;
                    </button>
                </div>

                <div style={{ marginBottom: '12px', position: 'relative', zIndex: 10 }}>
                    <label style={labelStyle}>Date</label>
                    <DatePicker
                        selected={selectedDate}
                        onChange={(d) => setSelectedDate(d)}
                        minDate={new Date()}
                        filterDate={isSelectableDate}
                        dateFormat="MM/dd/yyyy"
                        placeholderText="Select date"
                        customInput={<DateSelectLikeInput placeholder="Select date" />}
                        popperPlacement="bottom-start"
                        popperModifiers={popperModifiers}
                        calendarClassName="ep-datepicker"
                        popperClassName="ep-datepicker-popper"
                    />
                </div>

                <div style={{ marginBottom: '10px', position: 'relative', zIndex: 1 }}>
                    <label style={labelStyle}>Time Range</label>
                    <TimeRangeSlider
                        startIdx={startIdx}
                        endIdx={endIdx}
                        onRequestStartIdx={onRequestStartIdx}
                        onRequestEndIdx={onRequestEndIdx}
                        overlayUnavailableAtIndex={overlayUnavailableAtIndex}
                        selectedDate={selectedDate}
                    />
                    <div style={legendText}>
                        <span style={legendDot} />
                        Red areas are unavailable (already booked or outside the owner&apos;s hours).
                    </div>
                    {busyLoading && (
                        <div style={{ marginTop: 8, fontSize: 12, color: '#64748b', textAlign: 'center' }}>
                            Loading busy times...
                        </div>
                    )}
                </div>

                <div
                    style={{
                        background: '#f8fafc',
                        padding: '15px',
                        borderRadius: '12px',
                    }}
                >
                    <div>
                        <div style={{ fontSize: '12px', color: '#64748b' }}>Total Estimate</div>
                        <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#0f172a' }}>
                            {totalPrice > 0 ? `₪${totalPrice}` : '--'}
                        </div>
                    </div>

                    <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
                        <button
                            onClick={handleConfirm}
                            disabled={saving || savingAdd || !!errorMsg || !totalPrice}
                            style={{
                                width: '100%',
                                padding: '12px 24px',
                                background: saving || savingAdd || !!errorMsg ? '#94a3b8' : '#0f172a',
                                color: 'white',
                                border: 'none',
                                borderRadius: '8px',
                                fontWeight: '700',
                                cursor: saving || savingAdd || !!errorMsg ? 'not-allowed' : 'pointer',
                            }}
                        >
                            {saving ? 'Booking...' : 'Confirm'}
                        </button>

                        <button
                            onClick={handleConfirmAndAddAnother}
                            disabled={savingAdd || saving || !!errorMsg || !totalPrice}
                            style={{
                                width: '100%',
                                padding: '12px 16px',
                                background: savingAdd || saving || !!errorMsg ? '#cbd5e1' : '#0F172A',
                                color: 'white',
                                border: 'none',
                                borderRadius: '8px',
                                fontWeight: '700',
                                cursor: savingAdd || saving || !!errorMsg ? 'not-allowed' : 'pointer',
                            }}
                        >
                            {savingAdd ? 'Booking...' : 'Confirm & Add Another Slot'}
                        </button>
                    </div>
                </div>

                {errorMsg && (
                    <div
                        style={{
                            color: '#dc2626',
                            fontSize: '13px',
                            marginTop: '10px',
                            textAlign: 'center',
                            background: '#fee2e2',
                            padding: '8px',
                            borderRadius: '6px',
                        }}
                    >
                        {errorMsg}
                    </div>
                )}

                {feedback.isError && !errorMsg && (
                    <div
                        style={{
                            color: '#dc2626',
                            fontSize: '13px',
                            marginTop: '10px',
                            textAlign: 'center',
                            background: '#fee2e2',
                            padding: '8px',
                            borderRadius: '6px',
                        }}
                    >
                        {feedback.message}
                    </div>
                )}

                {/* --- Temporary Alert Section --- */}
                {tempAlert && (
                    <div
                        style={{
                            color: '#b45309', // Darker yellow/orange
                            fontSize: '13px',
                            marginTop: '10px',
                            textAlign: 'center',
                            background: '#fffbeb', // Light yellow
                            padding: '8px',
                            borderRadius: '6px',
                        }}
                    >
                        {tempAlert}
                    </div>
                )}
            </div>
        </div>
    )
}

// ---------- Styles ----------
const labelStyle = {
    display: 'block',
    fontSize: '12px',
    fontWeight: '600',
    color: '#64748b',
    marginBottom: '4px',
}
const overlayStyle = {
    position: 'fixed',
    inset: 0,
    zIndex: 9999,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
}
const modalStyle = {
    position: 'relative',
    background: 'white',
    width: '90%',
    maxWidth: '520px',
    padding: '25px',
    borderRadius: '20px',
    boxShadow: '0 25px 50px -12px rgba(0,0,0,0.25)',
}
const closeBtnStyle = {
    background: 'none',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#cbd5e1',
}

const selectStyle = {
    width: '100%',
    height: '48px',
    padding: '0 12px',
    paddingRight: '35px',
    borderRadius: '10px',
    border: '1px solid #e2e8f0',
    fontSize: '16px',
    outline: 'none',
    color: '#0f172a',
    backgroundColor: '#fff',
    cursor: 'pointer',
    appearance: 'none',
    backgroundImage: `url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%2364748b%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E")`,
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'right 12px center',
    backgroundSize: '12px',
    boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
}

const timeSummaryRow = { display: 'flex', gap: 10, justifyContent: 'space-between', alignItems: 'center' }
const timePill = {
    flex: 1,
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    background: '#f8fafc',
    border: '1px solid #e2e8f0',
    borderRadius: 12,
    padding: '10px 12px',
}
const dotStyle = { width: 10, height: 10, borderRadius: 999, display: 'inline-block' }
const timePillLabel = { fontSize: 12, color: '#64748b', fontWeight: 800 }
const timePillValue = {
    marginLeft: 'auto',
    fontSize: 18,
    fontWeight: 900,
    color: '#0f172a',
    letterSpacing: 0.2,
}

const durationPill = {
    display: 'inline-block',
    fontSize: 12,
    color: '#64748b',
    background: '#f8fafc',
    border: '1px solid #e2e8f0',
    padding: '6px 10px',
    borderRadius: 999,
    fontWeight: 700,
}

const legendText = {
    marginTop: 10,
    fontSize: 12,
    color: '#64748b',
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    justifyContent: 'center',
}
const legendDot = {
    width: 10,
    height: 10,
    borderRadius: 999,
    background: '#fecaca',
    border: '1px solid rgba(239, 68, 68, 0.35)',
    display: 'inline-block',
}

const globalCss = `
.react-datepicker-popper { z-index: 20000 !important; }
.react-datepicker { z-index: 20000 !important; }
.react-datepicker__triangle { display: none !important; }

.ep-datepicker-popper { z-index: 20000 !important; }
.ep-datepicker {
  font-family: inherit;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}
.ep-datepicker .react-datepicker__triangle { display: none; }
.ep-datepicker .react-datepicker__header {
  background: #ffffff;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  padding: 10px 10px 8px;
}
.ep-datepicker .react-datepicker__current-month {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 6px;
}
.ep-datepicker .react-datepicker__day-name {
  color: #64748b;
  font-weight: 700;
  font-size: 11px;
  width: 30px;
  line-height: 26px;
  margin: 0;
}
.ep-datepicker .react-datepicker__month {
  margin: 0;
  padding: 10px;
  background: #ffffff;
}
.ep-datepicker .react-datepicker__day {
  width: 30px;
  line-height: 30px;
  margin: 0;
  border-radius: 10px;
  font-weight: 700;
  color: #0f172a;
  font-size: 12px;
}
.ep-datepicker .react-datepicker__day:hover { background: rgba(15, 23, 42, 0.06); }
.ep-datepicker .react-datepicker__day--today {
  box-shadow: inset 0 0 0 2px rgba(34, 197, 94, 0.6);
  border-radius: 10px;
}
.ep-datepicker .react-datepicker__day--selected,
.ep-datepicker .react-datepicker__day--keyboard-selected {
  background: #0f172a;
  color: #ffffff;
  border-radius: 10px;
}
.ep-datepicker .react-datepicker__day--disabled { color: rgba(100, 116, 139, 0.4); }

.ep-range {
  position: absolute;
  left: 0; right: 0; top: 0; bottom: 0;
  width: 100%;
  height: 60px;
  background: transparent;
  -webkit-appearance: none;
  appearance: none;
  outline: none;
  pointer-events: none;
}
.ep-range::-webkit-slider-runnable-track { background: transparent; height: 60px; }
.ep-range::-moz-range-track { background: transparent; height: 60px; }
.ep-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #0f172a;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.25);
  pointer-events: all;
  cursor: pointer;
}
.ep-range::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #0f172a;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.25);
  pointer-events: all;
  cursor: pointer;
}
`