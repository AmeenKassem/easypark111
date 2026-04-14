import React, { useEffect, useState, useMemo } from 'react';
import Layout from '../components/layout/layout';
import { getDriverReport } from '../services/report';
import TimeDropdown from '../components/inputs/TimeDropdown';


const getMonthYear = (dateString) => {
    if (!dateString) return null;
    const d = new Date(dateString);
    return `${d.getMonth() + 1}/${d.getFullYear()}`;
};


const generateLast12Months = () => {
    const months = [];
    const now = new Date();
    for (let i = 0; i < 12; i++) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        months.push(`${d.getMonth() + 1}/${d.getFullYear()}`);
    }
    return months;
};


const labelStyle = {
    fontSize: '11px',
    fontWeight: '800',
    color: '#64748b',
    marginBottom: '6px',
    display: 'block',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
};

export default function ExpensesPage() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [selectedMonth, setSelectedMonth] = useState('All Time');


    const [invoiceBooking, setInvoiceBooking] = useState(null);

    useEffect(() => {
        const loadData = async () => {
            try {
                const data = await getDriverReport();
                setStats(data);
            } catch (error) {
                console.error("Failed to load driver expenses", error);
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, []);


    const dropdownOptions = useMemo(() => {
        return ['All Time', ...generateLast12Months()];
    }, []);

    //filter
    const filteredData = useMemo(() => {
        if (!stats) return { list: [], totalExpenses: 0, totalBookings: 0 };

        let filteredList = stats.bookingHistory || [];

        if (selectedMonth !== 'All Time') {
            filteredList = filteredList.filter(
                b => getMonthYear(b.startTime) === selectedMonth
            );
        }

        const totalE = filteredList.reduce((sum, b) => {
            if (String(b.status || '').toUpperCase() === 'APPROVED') {
                return sum + (Number(b.totalPrice) || 0);
            }
            return sum;
        }, 0);

        return {
            list: filteredList,
            totalExpenses: totalE,
            totalBookings: filteredList.length
        };
    }, [stats, selectedMonth]);

    const fmtDate = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '';

    if (loading) return <Layout><div style={{padding: 20}}>Loading statistics...</div></Layout>;
    if (!stats) return <Layout><div style={{padding: 20}}>No data available.</div></Layout>;

    const { list, totalExpenses, totalBookings } = filteredData;

    return (
        <Layout title="My Expenses">

            <style>{`
                @media print {
                    body * { visibility: hidden; }
                    #printable-invoice, #printable-invoice * { visibility: visible; }
                    #printable-invoice { position: absolute; left: 0; top: 0; width: 100%; box-shadow: none; margin: 0; padding: 20px; border: none; }
                    .no-print { display: none !important; }
                }
            `}</style>

            <div style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>


                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 25 }}>
                    <div style={{ width: '150px' }}>
                        <label style={labelStyle}>Filter By Month</label>
                        <TimeDropdown
                            value={selectedMonth}
                            onChange={(v) => setSelectedMonth(v)}
                            options={dropdownOptions}
                            placeholder="Select Month"
                            maxVisible={5}
                        />
                    </div>
                </div>


                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 15, marginBottom: 30 }}>
                    <div style={{ background: '#fee2e2', padding: 20, borderRadius: 12, textAlign: 'center', color: '#991b1b' }}>
                        <h3 style={{ margin: 0, fontSize: 14, textTransform: 'uppercase' }}>Total Expenses</h3>
                        <div style={{ fontSize: 32, fontWeight: 900 }}>₪{totalExpenses?.toFixed(2) || 0}</div>
                    </div>

                    <div style={{ background: '#e0f2fe', padding: 20, borderRadius: 12, textAlign: 'center', color: '#075985' }}>
                        <h3 style={{ margin: 0, fontSize: 14, textTransform: 'uppercase' }}>Total Bookings</h3>
                        <div style={{ fontSize: 32, fontWeight: 900 }}>{totalBookings || 0}</div>
                    </div>
                </div>

                <h3>Booking History</h3>
                <div style={{ display: 'grid', gap: 10 }}>
                    {list && list.length > 0 ? list.map((item) => {
                        const isApproved = String(item.status || '').toUpperCase() === 'APPROVED';

                        return (
                            <div key={item.id} style={{ border: '1px solid #e2e8f0', borderRadius: 10, padding: 15, background: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div>
                                    <div style={{ fontWeight: 'bold', color: '#0f172a' }}>Parking ID: {item.parkingId}</div>
                                    <div style={{ fontSize: 12, color: '#64748b', marginTop: '4px' }}>
                                        {fmtDate(item.startTime)} - {fmtDate(item.endTime)}
                                    </div>

                                    {isApproved && (
                                        <button
                                            onClick={() => setInvoiceBooking(item)}
                                            style={{ marginTop: '10px', background: '#f8fafc', border: '1px solid #cbd5e1', color: '#334155', padding: '4px 10px', borderRadius: '6px', fontSize: '11px', fontWeight: 'bold', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
                                        >
                                            📄 View Invoice
                                        </button>
                                    )}
                                </div>
                                <div style={{ textAlign: 'right' }}>
                                    <div style={{ fontWeight: 900, color: '#1e293b' }}>
                                        -₪{item.totalPrice}
                                    </div>
                                    <span style={{ fontSize: 10, padding: '2px 6px', borderRadius: 4, background: '#f1f5f9', display: 'inline-block', marginTop: '4px' }}>
                                        {item.status}
                                    </span>
                                </div>
                            </div>
                        );
                    }) : (
                        <div style={{ textAlign: 'center', padding: '30px', background: '#f8fafc', borderRadius: '10px', border: '1px dashed #cbd5e1' }}>
                            <p style={{ color: '#64748b', fontWeight: 600, margin: 0 }}>No bookings in this month.</p>
                        </div>
                    )}
                </div>
            </div>


            {invoiceBooking && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(15, 23, 42, 0.75)', zIndex: 99999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backdropFilter: 'blur(4px)' }}>

                    <div id="printable-invoice" style={{ background: '#fff', width: '100%', maxWidth: '420px', borderRadius: '16px', padding: '30px', position: 'relative', boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)' }}>


                        <button className="no-print" onClick={() => setInvoiceBooking(null)} style={{ position: 'absolute', top: '15px', right: '15px', background: 'transparent', border: 'none', fontSize: '20px', color: '#94a3b8', cursor: 'pointer' }}>✖</button>

                        {/* invoice */}
                        <div style={{ textAlign: 'center', borderBottom: '2px dashed #e2e8f0', paddingBottom: '20px', marginBottom: '20px' }}>
                            <div style={{ fontWeight: 900, fontSize: '24px', color: '#2563eb', marginBottom: '5px' }}>EasyPark</div>
                            <div style={{ fontSize: '14px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '1px' }}>Receipt / Tax Invoice</div>
                        </div>

                        {/* details */}
                        <div style={{ display: 'grid', gap: '12px', fontSize: '14px', color: '#334155', marginBottom: '25px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#64748b' }}>Receipt No:</span>
                                <span style={{ fontWeight: 700 }}>INV-{invoiceBooking.id}-{new Date(invoiceBooking.startTime).getFullYear()}</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#64748b' }}>Date Issued:</span>
                                <span style={{ fontWeight: 700 }}>{new Date().toLocaleDateString()}</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#64748b' }}>Parking ID:</span>
                                <span style={{ fontWeight: 700 }}>#{invoiceBooking.parkingId}</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: '#64748b' }}>Duration:</span>
                                <span style={{ fontWeight: 700, textAlign: 'right' }}>
                                    {fmtDate(invoiceBooking.startTime)}<br/>to {fmtDate(invoiceBooking.endTime)}
                                </span>
                            </div>
                        </div>

                        {/* price */}
                        <div style={{ background: '#f8fafc', padding: '20px', borderRadius: '12px', textAlign: 'center' }}>
                            <div style={{ fontSize: '13px', color: '#64748b', textTransform: 'uppercase', fontWeight: 700, marginBottom: '5px' }}>Total Amount Paid</div>
                            <div style={{ fontSize: '36px', fontWeight: 900, color: '#0f172a', marginBottom: '15px' }}>₪{invoiceBooking.totalPrice}</div>

                            {/*  Bit */}
                            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', background: '#dcfce7', color: '#166534', padding: '6px 12px', borderRadius: '99px', fontSize: '13px', fontWeight: 800, border: '1px solid #bbf7d0' }}>
                                <span>✔</span> Paid via bit
                            </div>
                        </div>

                        <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '12px', color: '#94a3b8' }}>
                            Thank you for parking with EasyPark!
                        </div>

                        {/* print */}
                        <button className="no-print" onClick={() => window.print()} style={{ width: '100%', marginTop: '25px', background: '#0f172a', color: 'white', border: 'none', padding: '14px', borderRadius: '10px', fontWeight: 700, fontSize: '14px', cursor: 'pointer', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}>
                            🖨️ Print / Save as PDF
                        </button>
                    </div>
                </div>
            )}
        </Layout>
    );
}