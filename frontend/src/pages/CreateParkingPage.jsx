import React, { useEffect, useState, useMemo } from 'react';

import axios from 'axios';
import AddressAutocomplete from '../components/forms/AddressAutocomplete';
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import TimeDropdown from '../components/inputs/TimeDropdown'
import { generateTimeOptions } from '../utils/timeOptions'
import {API_BASE_URL} from "../config.js";

const toYMD = (d) => {
    if (!d) return '';
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
};
const splitIsoToDateTime = (iso) => {
    if (!iso) return { date: '', time: '' }
    const s = String(iso)
    const [d, tRaw] = s.split('T')
    const t = (tRaw || '').slice(0, 5)
    return { date: d || '', time: t || '' }
}

const normalizeTimeHHMM = (t) => {
    if (!t) return ''
    return String(t).slice(0, 5)
}

const CreateParkingPage = ({ onClose, onCreated, onUpdated, mode = 'create', initialSpot = null }) => {

    const [formData, setFormData] = useState({
        location: '',
        description: '',
        lat: null,
        lng: null,
        pricePerHour: '',
        covered: false,
    });

    const [availabilityType, setAvailabilityType] = useState('specific');

    const [specificSlots, setSpecificSlots] = useState([
        { id: Date.now(), startDate: '', startTime: '', endDate: '', endTime: '' }
    ]);

    const [weeklySchedule, setWeeklySchedule] = useState({
        0: { active: false, start: '', end: '' },
        1: { active: false, start: '', end: '' },
        2: { active: false, start: '', end: '' },
        3: { active: false, start: '', end: '' },
        4: { active: false, start: '', end: '' },
        5: { active: false, start: '', end: '' },
        6: { active: false, start: '', end: '' },
    });

    const [batchTime, setBatchTime] = useState({ start: '', end: '' });
    const [loading, setLoading] = useState(false);
    const [apiMessage, setApiMessage] = useState('');

    useEffect(() => {
        if (mode !== 'edit' || !initialSpot) return

        setFormData({
            location: initialSpot.location || '',
            description: initialSpot.description || '',
            lat: initialSpot.lat ?? null,
            lng: initialSpot.lng ?? null,
            pricePerHour: String(initialSpot.pricePerHour ?? ''),
            covered: !!initialSpot.covered,
        })

        const t = String(initialSpot.availabilityType || '').toLowerCase()
        if (t === 'recurring') setAvailabilityType('recurring')
        else setAvailabilityType('specific')

        if (String(initialSpot.availabilityType || '').toUpperCase() === 'SPECIFIC') {
            const slots = Array.isArray(initialSpot.specificAvailability) ? initialSpot.specificAvailability : []
            const mapped = slots.length
                ? slots.map((s) => {
                    const start = splitIsoToDateTime(s.start)
                    const end = splitIsoToDateTime(s.end)
                    return {
                        id: Date.now() + Math.random(),
                        startDate: start.date,
                        startTime: start.time,
                        endDate: end.date,
                        endTime: end.time,
                    }
                })
                : [{ id: Date.now(), startDate: '', startTime: '', endDate: '', endTime: '' }]

            setSpecificSlots(mapped)
        }

        if (String(initialSpot.availabilityType || '').toUpperCase() === 'RECURRING') {
            const rec = Array.isArray(initialSpot.recurringSchedule) ? initialSpot.recurringSchedule : []
            const base = {
                0: { active: false, start: '', end: '' },
                1: { active: false, start: '', end: '' },
                2: { active: false, start: '', end: '' },
                3: { active: false, start: '', end: '' },
                4: { active: false, start: '', end: '' },
                5: { active: false, start: '', end: '' },
                6: { active: false, start: '', end: '' },
            }

            for (const r of rec) {
                const d = Number(r.dayOfWeek)
                if (!Number.isFinite(d) || d < 0 || d > 6) continue
                base[d] = {
                    active: true,
                    start: normalizeTimeHHMM(r.start),
                    end: normalizeTimeHHMM(r.end),
                }
            }

            setWeeklySchedule(base)
        }
    }, [mode, initialSpot])

    const timeOptions = useMemo(() => generateTimeOptions(30), []);

    const getCurrentTimeHHMM = () => {
        const now = new Date();
        return `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}`;
    };

    const validationErrors = useMemo(() => {
        const errors = [];
        if (!formData.lat || !formData.lng) errors.push("Please select a precise location from the list.");
        if (!formData.pricePerHour || parseFloat(formData.pricePerHour) <= 0) errors.push("Price per hour must be greater than 0.");

        if (availabilityType === 'specific') {
            const invalidSlots = specificSlots.some(s => !s.startDate || !s.endDate || !s.startTime || !s.endTime);
            if (invalidSlots) errors.push("Please complete all start and end dates/times.");
        } else {
            const activeDays = Object.values(weeklySchedule).filter(d => d.active);
            if (activeDays.length === 0) {
                errors.push("Please select at least one active day.");
            } else if (activeDays.some(d => !d.start || !d.end)) {
                errors.push("Please set start and end times for all selected days.");
            }
        }
        return errors;
    }, [formData, availabilityType, specificSlots, weeklySchedule]);

    const getStartOptions = () => timeOptions.filter(t => t !== '23:59');

    const getValidStartTimes = (selectedDate) => {
        let options = getStartOptions();
        if (!selectedDate) return options;

        const todayStr = new Date().toISOString().split('T')[0];
        if (selectedDate === todayStr) {
            const currentHm = getCurrentTimeHHMM();
            return options.filter(t => t > currentHm);
        }
        return options;
    };

    const getValidEndTimes = (startDate, endDate, startTime) => {
        if (startDate && endDate && startDate === endDate) {
            return timeOptions.filter(t => t > startTime);
        }
        return timeOptions;
    };

    const getValidRecurringEndTimes = (startTime) => {
        if (!startTime) return timeOptions;
        return timeOptions.filter(t => t > startTime);
    };

    const handleAddressSelect = ({ lat, lng, address, address_components }) => {
        setApiMessage('');
        if (address_components) {
            const hasStreetNumber = address_components.some(component => component.types.includes('street_number'));
            if (!hasStreetNumber) {
                setApiMessage('⚠️ Please select a precise address that includes a street number.');
                setFormData(prev => ({ ...prev, lat: null, lng: null, location: address }));
                return;
            }
        }
        setFormData(prev => ({ ...prev, lat, lng, location: address }));
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    };

    const addSpecificSlot = () => {
        setSpecificSlots(prev => [...prev, { id: Date.now(), startDate: '', startTime: '', endDate: '', endTime: '' }]);
    };

    const removeSpecificSlot = (id) => {
        if (specificSlots.length === 1) return;
        setSpecificSlots(prev => prev.filter(slot => slot.id !== id));
    };

    const updateSpecificSlot = (id, field, value) => {
        setSpecificSlots(prev => prev.map(slot => {
            if (slot.id !== id) return slot;

            const newSlot = { ...slot, [field]: value };
            const todayStr = new Date().toISOString().split('T')[0];
            const currentTime = getCurrentTimeHHMM();

            if (newSlot.startDate === todayStr && newSlot.startTime) {
                if (newSlot.startTime <= currentTime) {
                    newSlot.startTime = '';
                }
            }

            if (newSlot.startDate && newSlot.endDate) {
                if (newSlot.endDate < newSlot.startDate) {
                    newSlot.endDate = '';
                    newSlot.endTime = '';
                }
            }

            if (newSlot.startDate && newSlot.endDate && newSlot.startDate === newSlot.endDate) {
                if (newSlot.startTime && newSlot.endTime) {
                    if (newSlot.endTime <= newSlot.startTime) {
                        newSlot.endTime = '';
                    }
                }
            }

            return newSlot;
        }));
    };

    const toggleDay = (dayIndex) => {
        setWeeklySchedule(prev => ({ ...prev, [dayIndex]: { ...prev[dayIndex], active: !prev[dayIndex].active } }));
    };

    const updateDayTime = (dayIndex, field, value) => {
        setWeeklySchedule(prev => {
            const newData = { ...prev[dayIndex], [field]: value };
            if (field === 'start' && newData.end && value >= newData.end) {
                newData.end = '';
            }
            return { ...prev, [dayIndex]: newData };
        });
    };

    const handleBatchTimeChange = (field, value) => {
        setBatchTime(prev => {
            const newState = { ...prev, [field]: value };
            if (field === 'start' && newState.end && value >= newState.end) {
                newState.end = '';
            }
            return newState;
        });
    };

    const applyBatchTime = () => {
        if (!batchTime.start || !batchTime.end) {
            setApiMessage('Please select valid Quick Apply times first.');
            return;
        }
        setWeeklySchedule(prev => {
            const newState = { ...prev };
            Object.keys(newState).forEach(key => {
                if (newState[key].active) {
                    newState[key].start = batchTime.start;
                    newState[key].end = batchTime.end;
                }
            });
            return newState;
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (validationErrors.length > 0) return;

        setLoading(true);
        setApiMessage('');

        try {
            const token = localStorage.getItem('easypark_token');
            if (!token) {
                setApiMessage('You must be logged in to create a parking spot.');
                setLoading(false);
                return;
            }

            let payload = {
                location: formData.location,

                description: formData.description ? formData.description.trim() : null,
                lat: formData.lat,
                lng: formData.lng,
                pricePerHour: parseFloat(formData.pricePerHour),
                covered: formData.covered,
                availabilityType: availabilityType.toUpperCase()
            };

            if (availabilityType === 'specific') {
                const formattedSlots = specificSlots.map(slot => ({
                    start: `${slot.startDate}T${slot.startTime}:00`,
                    end: `${slot.endDate}T${slot.endTime}:00`
                }));
                payload.specificAvailability = formattedSlots;
            } else {
                const scheduleList = Object.keys(weeklySchedule)
                    .map((key) => {
                        const dayOfWeek = parseInt(key)
                        const d = weeklySchedule[key]
                        if (!d.active) return null
                        return {
                            dayOfWeek,
                            start: d.start,
                            end: d.end,
                        }
                    })
                    .filter(Boolean)
                payload.recurringSchedule = scheduleList
            }

            const API_BASE = API_BASE_URL

            if (mode === 'edit') {
                if (!initialSpot?.id) throw new Error('Missing spot id for edit.')

                await axios.put(`${API_BASE}/api/parking-spots/${initialSpot.id}`, payload, {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                })

                setApiMessage('Success! Parking spot updated.')
                setTimeout(() => { onUpdated?.(); onClose?.(); }, 700)
            } else {
                await axios.post(`${API_BASE}/api/parking-spots`, payload, {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                })

                setApiMessage('Success! Parking spot created.')
                setTimeout(() => { onCreated?.(); onClose?.(); }, 700)
            }

        } catch (error) {
            const apiMsg = error?.response?.data?.message || error?.response?.data?.error;
            setApiMessage(apiMsg ? `Error: ${apiMsg}` : 'Error creating parking spot.');
        } finally {
            setLoading(false);
        }
    };

    const daysLabels = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
    const daysFullNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

    return (
        <div style={{ maxWidth: '650px', margin: '40px auto', padding: '30px', boxShadow: '0 10px 40px rgba(0,0,0,0.08)', borderRadius: '20px', backgroundColor: '#ffffff', color: '#1e293b', fontFamily: '"Segoe UI", Roboto, Helvetica, Arial, sans-serif' }}>
            <style>{`
              input[type=number]::-webkit-outer-spin-button,
              input[type=number]::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
              input[type=number] { -moz-appearance: textfield; }

              .ep-date-wrap { width: 100%; }
              .ep-date-picker {
                width: 100%;
                height: 38px;
                padding: 0 10px;
                border-radius: 8px;
                border: 1px solid #e2e8f0;
                font-size: 13px;
                outline: none;
                color: #1e293b;
                font-family: inherit;
                background: #fff;
                box-sizing: border-box;
              }
              .ep-date-picker:focus {
                border-color: #94a3b8;
              }

              .react-datepicker {
                border-radius: 12px;
                border: 1px solid #e2e8f0;
                box-shadow: 0 12px 30px rgba(0,0,0,0.12);
                overflow: hidden;
              }
              .react-datepicker__header {
                background: #ffffff;
                border-bottom: 1px solid #f1f5f9;
              }
            `}</style>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '25px' }}>
                <h2 style={{ margin: 0, fontWeight: '800', fontSize: '24px' }}>
                    {mode === 'edit' ? 'Edit Spot Availability' : 'Add New Spot'}
                </h2>
                <button type="button" onClick={onClose} style={{ border: 'none', background: 'none', fontSize: '24px', cursor: 'pointer', color: '#94a3b8' }}>&times;</button>
            </div>

            <form onSubmit={handleSubmit}>
                <div style={groupStyle}>
                    <label style={labelStyle}>Location</label>

                    {mode === 'edit' ? (
                        <>
                            <input
                                value={formData.location || ''}
                                disabled
                                readOnly
                                style={{
                                    ...inputStyle,
                                    backgroundColor: '#f8fafc',
                                    cursor: 'not-allowed',
                                    color: '#334155',
                                }}
                            />
                            <div style={{ marginTop: '8px', fontSize: '12px', color: '#64748b' }}>
                                Location cannot be changed when editing. Create a new spot if you need a different address.
                            </div>
                        </>
                    ) : (
                        <>
                            <AddressAutocomplete
                                onAddressSelect={handleAddressSelect}
                                options={{ types: ['address'], componentRestrictions: { country: 'il' } }}
                            />
                            {formData.location && formData.lat && (
                                <div style={{ marginTop: '8px', fontSize: '13px', color: '#10b981', display: 'flex', alignItems: 'center', gap: '4px' }}>
                                    <span>📍</span> {formData.location}
                                </div>
                            )}
                            {apiMessage && apiMessage.includes('precise') && (
                                <div style={{ marginTop: '5px', fontSize: '13px', color: '#ef4444', fontWeight: 'bold' }}>
                                    {apiMessage}
                                </div>
                            )}
                        </>
                    )}
                </div>


                <div style={{ ...groupStyle, marginTop: '12px' }}>
                    <label style={labelStyle}>Description (Optional, up to 80 chars)</label>
                    <input
                        type="text"
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        maxLength={80}
                        style={inputStyle}
                        placeholder="e.g., Covered parking spot near the entrance"
                    />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px', marginTop: '12px' }}>
                    <div style={groupStyle}>
                        <label style={labelStyle}>Price / Hour</label>
                        <div style={{...inputWrapperStyle, padding: '0 8px'}}>
                            <button type="button" onClick={() => { const newVal = parseFloat(formData.pricePerHour || 0) - 0.5; if (newVal >= 0) handleChange({ target: { name: 'pricePerHour', value: newVal } }); }} style={stepperBtnStyle}>−</button>
                            <div style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '2px' }}>
                                <input type="number" name="pricePerHour" value={formData.pricePerHour} onChange={handleChange} placeholder="0" style={{ ...inputStyle, border: 'none', textAlign: 'right', fontSize: '18px', fontWeight: 'bold', width: '45px', padding: 0, backgroundColor: 'transparent' }} />
                                <span style={{ fontSize: '16px', fontWeight: 'bold', color: '#64748b' }}>₪</span>
                            </div>
                            <button type="button" onClick={() => { const newVal = (parseFloat(formData.pricePerHour) || 0) + 0.5; handleChange({ target: { name: 'pricePerHour', value: newVal } }); }} style={stepperBtnStyle}>+</button>
                        </div>
                    </div>
                    <div style={groupStyle}>
                        <label style={labelStyle}>Features</label>
                        <div onClick={() => setFormData(prev => ({ ...prev, covered: !prev.covered }))} style={{ ...inputWrapperStyle, justifyContent: 'space-between', padding: '0 15px', cursor: 'pointer', borderColor: formData.covered ? '#3b82f6' : '#e2e8f0', backgroundColor: formData.covered ? '#eff6ff' : '#fff' }}>
                            <span style={{ fontSize: '14px', fontWeight: '500', color: formData.covered ? '#1d4ed8' : '#64748b' }}>Covered</span>
                            <div style={{ width: '36px', height: '20px', backgroundColor: formData.covered ? '#3b82f6' : '#cbd5e1', borderRadius: '20px', position: 'relative', transition: 'all 0.2s' }}>
                                <div style={{ width: '16px', height: '16px', backgroundColor: 'white', borderRadius: '50%', position: 'absolute', top: '2px', left: formData.covered ? '18px' : '2px', transition: 'all 0.2s', boxShadow: '0 1px 2px rgba(0,0,0,0.2)' }} />
                            </div>
                        </div>
                    </div>
                </div>

                <div style={{ backgroundColor: '#f1f5f9', padding: '4px', borderRadius: '12px', display: 'flex', marginBottom: '24px' }}>
                    <button type="button" onClick={() => setAvailabilityType('specific')} style={availabilityType === 'specific' ? activeTabStyle : inactiveTabStyle}>Specific Dates</button>
                    <button type="button" onClick={() => setAvailabilityType('recurring')} style={availabilityType === 'recurring' ? activeTabStyle : inactiveTabStyle}>Weekly Schedule</button>
                </div>

                {availabilityType === 'specific' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                        <div style={{fontSize: '13px', color: '#64748b', marginBottom: '5px'}}>Set the start and end time for when the parking is available.</div>
                        {specificSlots.map((slot) => {
                            const validEndTimes = getValidEndTimes(slot.startDate, slot.endDate, slot.startTime);
                            const isCurrentEndTimeValid = !slot.endTime || validEndTimes.includes(slot.endTime);
                            const displayEndTime = isCurrentEndTimeValid ? slot.endTime : '';

                            return (
                                <div key={slot.id} style={{ display: 'grid', gridTemplateColumns: '1fr 20px 1fr auto', gap: '10px', alignItems: 'center', backgroundColor: '#f8fafc', padding: '12px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
                                    <div style={{display: 'flex', flexDirection: 'column', gap: '5px'}}>
                                        <span style={{...subLabelStyle, color: '#166534'}}>FROM (Start)</span>
                                        <DatePicker
                                            selected={slot.startDate ? new Date(`${slot.startDate}T00:00:00`) : null}
                                            onChange={(d) => updateSpecificSlot(slot.id, 'startDate', d ? toYMD(d) : '')}
                                            minDate={new Date()}
                                            dateFormat="MM/dd/yyyy"
                                            placeholderText="Select date"
                                            className="ep-date-picker"
                                            wrapperClassName="ep-date-wrap"
                                        />

                                        <TimeDropdown
                                            value={slot.startTime}
                                            onChange={(v) => updateSpecificSlot(slot.id, 'startTime', v)}
                                            options={getValidStartTimes(slot.startDate)}
                                            placeholder="--:--"
                                            disabled={!slot.startDate}
                                            maxVisible={5}
                                        />

                                    </div>
                                    <div style={{ textAlign: 'center', color: '#cbd5e1', fontSize: '18px' }}>➝</div>
                                    <div style={{display: 'flex', flexDirection: 'column', gap: '5px'}}>
                                        <span style={{...subLabelStyle, color: '#991b1b'}}>TO (End)</span>
                                        <DatePicker
                                            selected={slot.endDate ? new Date(`${slot.endDate}T00:00:00`) : null}
                                            onChange={(d) => updateSpecificSlot(slot.id, 'endDate', d ? toYMD(d) : '')}
                                            minDate={slot.startDate ? new Date(`${slot.startDate}T00:00:00`) : new Date()}
                                            dateFormat="MM/dd/yyyy"
                                            placeholderText="Select date"
                                            className="ep-date-picker"
                                            wrapperClassName="ep-date-wrap"
                                        />

                                        <TimeDropdown
                                            value={displayEndTime}
                                            onChange={(v) => updateSpecificSlot(slot.id, 'endTime', v)}
                                            options={validEndTimes}
                                            placeholder="--:--"
                                            disabled={!slot.startDate || !slot.endDate || !slot.startTime}
                                            maxVisible={5}
                                        />

                                    </div>
                                    {specificSlots.length > 1 && <button type="button" onClick={() => removeSpecificSlot(slot.id)} style={removeBtnStyle}>&times;</button>}
                                </div>
                            );
                        })}
                        <button type="button" onClick={addSpecificSlot} style={addBtnStyle}>+ Add Another Range</button>
                    </div>
                )}

                {availabilityType === 'recurring' && (
                    <div>
                        <label style={labelStyle}>Select Days</label>
                        <div style={{ display: 'flex', gap: '10px', marginBottom: '20px', justifyContent: 'center' }}>
                            {daysLabels.map((dayLabel, index) => (
                                <button key={index} type="button" onClick={() => toggleDay(index)} style={{ width: '42px', height: '42px', borderRadius: '12px', border: 'none', backgroundColor: weeklySchedule[index].active ? '#2563eb' : '#f1f5f9', color: weeklySchedule[index].active ? 'white' : '#64748b', fontWeight: '700', cursor: 'pointer', transition: 'all 0.2s', boxShadow: weeklySchedule[index].active ? '0 4px 12px rgba(37, 99, 235, 0.2)' : 'none' }}>{dayLabel}</button>
                            ))}
                        </div>
                        <div style={{ backgroundColor: '#f8fafc', padding: '16px', borderRadius: '12px', border: '1px dashed #cbd5e1', marginBottom: '20px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                                <span style={{ fontSize: '13px', fontWeight: '600', color: '#475569' }}>⚡ Quick Apply</span>
                                <button type="button" onClick={applyBatchTime} style={tinyBtnStyle}>Apply to Selected</button>
                            </div>
                            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                                <TimeDropdown
                                    value={batchTime.start}
                                    onChange={(v) => handleBatchTimeChange('start', v)}
                                    options={getStartOptions()}
                                    placeholder="--:--"
                                    maxVisible={5}
                                />
                                <span style={{ color: '#94a3b8' }}>➜</span>
                                <TimeDropdown
                                    value={batchTime.end}
                                    onChange={(v) => handleBatchTimeChange('end', v)}
                                    options={getValidRecurringEndTimes(batchTime.start)}
                                    placeholder="--:--"
                                    disabled={!batchTime.start}
                                    maxVisible={5}
                                />
                            </div>
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '250px', overflowY: 'auto', paddingRight: '4px' }}>
                            {Object.keys(weeklySchedule).map((key) => {
                                const dayIndex = parseInt(key);
                                const dayData = weeklySchedule[dayIndex];
                                if (!dayData.active) return null;
                                return (
                                    <div key={dayIndex} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', backgroundColor: '#fff', border: '1px solid #f1f5f9', borderRadius: '8px' }}>
                                        <div style={{ width: '80px', fontSize: '14px', fontWeight: '600', color: '#334155' }}>{daysFullNames[dayIndex]}</div>
                                        <TimeDropdown
                                            value={dayData.start}
                                            onChange={(v) => updateDayTime(dayIndex, 'start', v)}
                                            options={getStartOptions()}
                                            placeholder="--:--"
                                            maxVisible={5}
                                        />
                                        <span style={{ color: '#cbd5e1' }}>-</span>
                                        <TimeDropdown
                                            value={dayData.end}
                                            onChange={(v) => updateDayTime(dayIndex, 'end', v)}
                                            options={getValidRecurringEndTimes(dayData.start)}
                                            placeholder="--:--"
                                            disabled={!dayData.start}
                                            maxVisible={5}
                                        />
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {validationErrors.length > 0 && (
                    <div style={{ marginTop: '20px', padding: '12px', backgroundColor: '#fef2f2', borderRadius: '8px', border: '1px solid #fee2e2' }}>
                        <div style={{ fontSize: '13px', fontWeight: 'bold', color: '#b91c1c', marginBottom: '6px' }}>⚠️ Please fill in missing details:</div>
                        <ul style={{ margin: 0, paddingLeft: '20px', color: '#b91c1c', fontSize: '12px' }}>
                            {validationErrors.map((err, idx) => <li key={idx} style={{ marginBottom: '2px' }}>{err}</li>)}
                        </ul>
                    </div>
                )}

                <div style={{ display: 'flex', gap: '12px', marginTop: '20px', paddingTop: '20px', borderTop: '1px solid #f1f5f9' }}>
                    <button type="button" onClick={onClose} disabled={loading} style={cancelButtonStyle}>Cancel</button>
                    <button type="submit" disabled={loading || validationErrors.length > 0} style={{ ...submitButtonStyle, opacity: (loading || validationErrors.length > 0) ? 0.5 : 1, cursor: (loading || validationErrors.length > 0) ? 'not-allowed' : 'pointer' }}>
                        {loading ? 'Creating...' : 'Confirm'}
                    </button>
                </div>

                {apiMessage && !apiMessage.includes('precise') && (
                    <div style={{ marginTop: '15px', padding: '10px', textAlign: 'center', borderRadius: '8px', backgroundColor: apiMessage.includes('Success') ? '#dcfce7' : '#fee2e2', color: apiMessage.includes('Success') ? '#166534' : '#991b1b', fontSize: '14px', fontWeight: '500' }}>{apiMessage}</div>
                )}
            </form>
        </div>
    );
};

const groupStyle = { display: 'flex', flexDirection: 'column', gap: '6px' };
const labelStyle = { fontSize: '13px', fontWeight: '600', color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' };
const subLabelStyle = { fontSize: '11px', fontWeight: '700', color: '#94a3b8', marginBottom: '2px', textTransform: 'uppercase' };
const inputWrapperStyle = { display: 'flex', alignItems: 'center', border: '1px solid #e2e8f0', borderRadius: '10px', height: '42px', backgroundColor: '#fff', transition: 'border-color 0.2s' };
const inputStyle = { width: '100%', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '0 10px', height: '42px', fontSize: '14px', outline: 'none', color: '#1e293b', fontFamily: 'inherit' };
const stepperBtnStyle = { border: 'none', background: 'transparent', color: '#64748b', fontSize: '18px', padding: '0 10px', cursor: 'pointer' };
const activeTabStyle = { flex: 1, padding: '10px', borderRadius: '8px', border: 'none', backgroundColor: '#fff', boxShadow: '0 2px 5px rgba(0,0,0,0.05)', fontWeight: '600', color: '#0f172a', cursor: 'default' };
const inactiveTabStyle = { flex: 1, padding: '10px', borderRadius: '8px', border: 'none', backgroundColor: 'transparent', color: '#64748b', cursor: 'pointer', fontWeight: '500' };
const addBtnStyle = { marginTop: '10px', padding: '12px', border: '1px dashed #3b82f6', borderRadius: '12px', backgroundColor: '#eff6ff', color: '#2563eb', fontWeight: '600', cursor: 'pointer', width: '100%', fontSize: '14px' };
const removeBtnStyle = { border: 'none', background: 'none', color: '#ef4444', fontSize: '24px', cursor: 'pointer', display: 'flex', alignItems: 'center' };
const tinyBtnStyle = { fontSize: '11px', padding: '4px 10px', backgroundColor: '#3b82f6', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: '600' };
const cancelButtonStyle = { padding: '12px 24px', backgroundColor: '#fff', color: '#64748b', border: '1px solid #e2e8f0', borderRadius: '10px', fontWeight: '600', cursor: 'pointer' };
const submitButtonStyle = { flex: 1, padding: '12px', backgroundColor: '#0f172a', color: 'white', border: 'none', borderRadius: '10px', fontWeight: '600', cursor: 'pointer', boxShadow: '0 4px 12px rgba(15, 23, 42, 0.2)' };

export default CreateParkingPage;