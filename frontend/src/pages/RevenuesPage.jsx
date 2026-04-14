import React, { useEffect, useState, useMemo } from 'react';
import Layout from '../components/layout/layout';
import { getOwnerDashboard } from '../services/report';
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

export default function RevenuesPage() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [selectedMonth, setSelectedMonth] = useState('All Time');

    useEffect(() => {
        const loadData = async () => {
            try {
                const data = await getOwnerDashboard();
                setStats(data);
            } catch (error) {
                console.error("Failed to load owner revenues", error);
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, []);


    const dropdownOptions = useMemo(() => {
        return ['All Time', ...generateLast12Months()];
    }, []);


    const filteredData = useMemo(() => {
        if (!stats) return { list: [], totalRevenue: 0, totalReservations: 0 };

        let filteredList = stats.recentActivity || [];


        if (selectedMonth !== 'All Time') {
            filteredList = filteredList.filter(
                b => getMonthYear(b.startTime) === selectedMonth
            );
        }


        const totalR = filteredList.reduce((sum, b) => {
            if (String(b.status || '').toUpperCase() === 'APPROVED') {
                return sum + (Number(b.totalPrice) || 0);
            }
            return sum;
        }, 0);

        return {
            list: filteredList,
            totalRevenue: totalR,
            totalReservations: filteredList.length
        };
    }, [stats, selectedMonth]);

    const fmtDate = (dt) => dt ? dt.replace('T', ' ').slice(0, 16) : '';

    if (loading) return <Layout><div style={{padding: 20}}>Loading statistics...</div></Layout>;
    if (!stats) return <Layout><div style={{padding: 20}}>No data available.</div></Layout>;

    const { list, totalRevenue, totalReservations } = filteredData;

    return (
        <Layout title="My Revenues">
            <div style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>

                {/* filter */}
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

                {/* design */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 15, marginBottom: 30 }}>
                    <div style={{ background: '#dcfce7', padding: 20, borderRadius: 12, textAlign: 'center', color: '#166534' }}>
                        <h3 style={{ margin: 0, fontSize: 14, textTransform: 'uppercase' }}>Total Revenue</h3>
                        <div style={{ fontSize: 32, fontWeight: 900 }}>₪{totalRevenue?.toFixed(2) || 0}</div>
                    </div>

                    <div style={{ background: '#e0f2fe', padding: 20, borderRadius: 12, textAlign: 'center', color: '#075985' }}>
                        <h3 style={{ margin: 0, fontSize: 14, textTransform: 'uppercase' }}>Total Reservations</h3>
                        <div style={{ fontSize: 32, fontWeight: 900 }}>{totalReservations || 0}</div>
                    </div>
                </div>

                <h3>Recent Activity</h3>
                <div style={{ display: 'grid', gap: 10 }}>
                    {list && list.length > 0 ? list.map((item) => (
                        <div key={item.id} style={{ border: '1px solid #e2e8f0', borderRadius: 10, padding: 15, background: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div>
                                <div style={{ fontWeight: 'bold' }}>Parking ID: {item.parkingId}</div>
                                <div style={{ fontSize: 12, color: '#64748b' }}>
                                    {fmtDate(item.startTime)} - {fmtDate(item.endTime)}
                                </div>
                            </div>
                            <div style={{ textAlign: 'right' }}>
                                <div style={{ fontWeight: 900, color: '#166534' }}>
                                    +₪{item.totalPrice}
                                </div>
                                <span style={{ fontSize: 10, padding: '2px 6px', borderRadius: 4, background: '#f1f5f9' }}>
                                    {item.status}
                                </span>
                            </div>
                        </div>
                    )) : (
                        <div style={{ textAlign: 'center', padding: '30px', background: '#f8fafc', borderRadius: '10px', border: '1px dashed #cbd5e1' }}>
                            <p style={{ color: '#64748b', fontWeight: 600, margin: 0 }}>No activity in this month.</p>
                        </div>
                    )}
                </div>
            </div>
        </Layout>
    );
}