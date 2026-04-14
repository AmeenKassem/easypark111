import React, { useEffect, useMemo, useRef, useState } from 'react'
import { GoogleMap, Marker, InfoWindow, useJsApiLoader } from '@react-google-maps/api'
import axios from 'axios'
import { getAuthToken } from "../../services/session";
import {API_BASE_URL} from "../../config.js";

const containerStyle = { width: '100%', height: '100%' }
const defaultCenter = { lat: 32.0853, lng: 34.7818 }

const locateBtnStyle = {
    position: 'absolute',
    bottom: '70px',
    left: '10px',
    zIndex: 10,
    backgroundColor: 'white',
    border: 'none',
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    boxShadow: '0 2px 6px rgba(0,0,0,0.3)',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 0,
}


const secondaryBtnStyle = {
    backgroundColor: '#f8fafc',
    color: '#334155',
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    cursor: 'pointer',
    fontWeight: 600,
    transition: 'background 0.2s',
}

const btnStyleWaze = {
    ...secondaryBtnStyle,
    padding: '8px 12px',
    flex: 1,
    fontSize: '13px',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    gap: '6px'
}

const btnStyleGoogle = {
    ...secondaryBtnStyle,
    padding: '8px 12px',
    flex: 1,
    fontSize: '13px',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
}

const btnStyleDetails = {
    ...secondaryBtnStyle,
    padding: '10px 12px',
    fontSize: '14px',
    width: '100%',
    marginBottom: '12px'
}


const btnStyleRequest = {
    backgroundColor: '#2563eb',
    color: 'white',
    border: 'none',
    padding: '12px 12px',
    borderRadius: '10px',
    cursor: 'pointer',
    fontWeight: 'bold',
    fontSize: '15px',
    width: '100%',
    boxShadow: '0 4px 6px -1px rgba(37, 99, 235, 0.2)',
    transition: 'opacity 0.2s, transform 0.1s'
}

function SpotDetailModal({ spot, onClose }) {
    if (!spot) return null;

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    const infoBoxStyle = {
        fontSize: '14px',
        color: '#475569',
        lineHeight: '1.5',
        background: '#f8fafc',
        padding: '12px',
        borderRadius: '10px',
        border: '1px solid #e2e8f0'
    };

    return (
        <div
            onClick={handleOverlayClick}
            style={{
                position: 'fixed',
                inset: 0,
                background: 'rgba(0,0,0,0.5)',
                zIndex: 999999,
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'center',
                backdropFilter: 'blur(3px)',
                cursor: 'pointer',
                paddingTop: '180px',
                paddingLeft: '20px',
                paddingRight: '20px',
                paddingBottom: '20px'
            }}
        >
            <div style={{
                background: 'white',
                padding: '24px',
                borderRadius: '20px',
                width: '100%',
                maxWidth: '400px',
                maxHeight: 'calc(100vh - 200px)',
                overflowY: 'auto',
                boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
                position: 'relative',
                cursor: 'default'
            }}>
                <button
                    onClick={onClose}
                    style={{
                        position: 'absolute',
                        top: '16px',
                        right: '16px',
                        background: 'transparent',
                        border: 'none',
                        fontSize: '20px',
                        cursor: 'pointer',
                        color: '#94a3b8',
                        padding: '4px'
                    }}
                >
                    ✕
                </button>

                <h2 style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '16px', color: '#0f172a', marginTop: 0 }}>
                    Spot Details
                </h2>

                <img
                    src="/spot.png"
                    alt="Parking Spot"
                    style={{
                        width: '100%',
                        height: 'auto',
                        borderRadius: '12px',
                        marginBottom: '16px',
                        objectFit: 'cover',
                        maxHeight: '200px',
                        border: '1px solid #f1f5f9'
                    }}
                />

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>

                    {spot.description && (
                        <div style={infoBoxStyle}>
                            <strong style={{ color: '#0f172a' }}>Description:</strong> {spot.description}
                        </div>
                    )}


                    {typeof spot.covered === 'boolean' && (
                        <div style={infoBoxStyle}>
                            <strong style={{ color: '#0f172a' }}>Covered:</strong> {spot.covered ? 'Yes' : 'No'}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
// ----------------------------------------------

export default function MapComponent({
                                         spots = null,
                                         center = defaultCenter,
                                         zoom = 13,
                                         onSpotClick = null,
                                         currentUserId = null,
                                         onMapLoad = null
                                     }) {

    const mapRef = useRef(null)
    const [apiSpots, setApiSpots] = useState([])
    const [selectedSpot, setSelectedSpot] = useState(null)


    const [detailModalSpot, setDetailModalSpot] = useState(null)

    const [myLocation, setMyLocation] = useState(null)
    const [mapCenter, setMapCenter] = useState(center)
    const [ratingMessage, setRatingMessage] = useState('')
    const [isSubmittingRating, setIsSubmittingRating] = useState(false)
    const { isLoaded, loadError } = useJsApiLoader({
        id: 'easypark-google-maps',
        googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_KEY,
    })

    useEffect(() => {
        setMapCenter(center)
        if (mapRef.current && center) {
            mapRef.current.panTo(center);
        }
    }, [center])

    useEffect(() => {
        if (Array.isArray(spots)) return

        const fetchSpots = async () => {
            try {
                const res = await axios.get(`${API_BASE_URL}/api/parking-spots/search`)
                const valid = (res.data || []).filter((s) => s.lat != null && s.lng != null && s?.active)
                setApiSpots(valid)
            } catch (e) {
                console.error('Error fetching parking spots:', e)
                setApiSpots([])
            }
        }

        fetchSpots()
    }, [spots])

    const handleLocateUser = () => {
        if (!navigator.geolocation) {
            alert("Geolocation is not supported by your browser.");
            return;
        }

        const onPositionSuccess = (position) => {
            const newPos = {
                lat: position.coords.latitude,
                lng: position.coords.longitude
            };

            setMyLocation(newPos);
            setMapCenter(newPos);

            if (mapRef.current) {
                mapRef.current.panTo(newPos);
                mapRef.current.setZoom(15);
            }
        };

        const onFinalError = (error) => {
            console.error("Geolocation final error:", error);
            switch(error.code) {
                case 1: alert("Location access denied. Please check your browser or OS settings."); break;
                case 2: alert("Position unavailable. Please ensure Wi-Fi is enabled."); break;
                case 3: alert("Request timed out. Please try again."); break;
                default: alert(`Error retrieving location: ${error.message}`);
            }
        };

        const highAccuracyOptions = {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 0
        };

        navigator.geolocation.getCurrentPosition(
            onPositionSuccess,
            (error) => {
                console.warn("High accuracy lookup failed. Attempting fallback...", error);

                if (error.code === 1) {
                    onFinalError(error);
                    return;
                }

                const lowAccuracyOptions = {
                    enableHighAccuracy: false,
                    timeout: 10000,
                    maximumAge: 0
                };

                navigator.geolocation.getCurrentPosition(
                    onPositionSuccess,
                    onFinalError,
                    lowAccuracyOptions
                );
            },
            highAccuracyOptions
        );
    };

    const markerSpots = useMemo(() => {
        const src = Array.isArray(spots) ? spots : apiSpots
        return (src || []).filter((s) => s?.lat != null && s?.lng != null)
    }, [spots, apiSpots])

    const options = useMemo(
        () => ({
            disableDefaultUI: true,
            zoomControl: false,
            fullscreenControl: false,
            streetViewControl: false,
            mapTypeControl: false,
            clickableIcons: false,
            gestureHandling: 'greedy',
        }),
        []
    )

    const handleNavigate = (lat, lng, app) => {
        if (app === 'waze') {
            window.open(`https://waze.com/ul?ll=${lat},${lng}&navigate=yes`, '_blank')
        } else {
            window.open(`https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`, '_blank')
        }
    }
    const onLoad = (map) => {
        mapRef.current = map
        if (onMapLoad) {
            onMapLoad(map);
        }
        setTimeout(() => {
            if (window.google?.maps?.event && mapRef.current) {
                window.google.maps.event.trigger(mapRef.current, 'resize')
            }
        }, 120)

        setTimeout(() => {
            handleLocateUser();
        }, 200);
    }

    const onUnmount = () => {
        mapRef.current = null
        setSelectedSpot(null)
    }

    const getObscuredAddress = (fullAddress) => {
        if (!fullAddress) return 'Parking spot';
        let safeAddr = fullAddress.replace(/\s+,/g, ',').replace(/\s\s+/g, ' ').trim();
        return `Parking at ${safeAddr}`;
    };

    if (loadError) {
        return <div style={{ position: 'absolute', inset: 0, background: '#fff' }} />
    }

    if (!isLoaded) {
        return <div style={{ position: 'absolute', inset: 0, background: '#fff' }} />
    }
    const isMine = Boolean(currentUserId && selectedSpot?.ownerId != null && Number(selectedSpot.ownerId) === Number(currentUserId));

    return (
        <div style={{ position: 'absolute', inset: 0 }}>
            <button
                onClick={handleLocateUser}
                style={locateBtnStyle}
                title="Show Your Location"
            >
                <svg
                    xmlns="http://www.w3.org/2000/svg"
                    height="24px"
                    viewBox="0 0 24 24"
                    width="24px"
                    fill="#444"
                >
                    <path d="M0 0h24v24H0V0z" fill="none"/>
                    <path d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3c-.46-4.17-3.77-7.48-7.94-7.94V1h-2v2.06C6.83 3.52 3.52 6.83 3.06 11H1v2h2.06c.46 4.17 3.77 7.48 7.94 7.94V23h2v-2.06c4.17-.46 7.48-3.77 7.94-7.94H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/>
                </svg>
            </button>

            <GoogleMap
                mapContainerStyle={containerStyle}
                center={mapCenter}
                zoom={zoom}
                options={options}
                onLoad={onLoad}
                onUnmount={onUnmount}
            >
                {myLocation && (
                    <>
                        <Marker
                            position={myLocation}
                            zIndex={1}
                            clickable={false}
                            icon={{
                                path: google.maps.SymbolPath.CIRCLE,
                                scale: 20,
                                fillColor: '#4285F4',
                                fillOpacity: 0.3,
                                strokeWeight: 0,
                            }}
                        />
                        <Marker
                            position={myLocation}
                            zIndex={2}
                            icon={{
                                path: google.maps.SymbolPath.CIRCLE,
                                scale: 8,
                                fillColor: '#4285F4',
                                fillOpacity: 1,
                                strokeColor: '#ffffff',
                                strokeWeight: 2,
                            }}
                        />
                    </>
                )}

                {markerSpots.map((spot) => (
                    <Marker
                        key={spot.id ?? `${spot.lat}-${spot.lng}`}
                        position={{ lat: Number(spot.lat), lng: Number(spot.lng) }}
                        onClick={() => {
                            setRatingMessage('')
                            setSelectedSpot(spot)
                        }}
                    />
                ))}

                {selectedSpot && (
                    <InfoWindow
                        position={{ lat: Number(selectedSpot.lat), lng: Number(selectedSpot.lng) }}
                        onCloseClick={() => {
                            setRatingMessage('')
                            setSelectedSpot(null)
                        }}
                    >
                        <div style={{ minWidth: 250 }}>
                            <h3 style={{ margin: '0 0 10px 0',color: 'black', fontSize: '15px' }}>
                                {getObscuredAddress(selectedSpot.location)}
                            </h3>

                            {selectedSpot.pricePerHour != null && (
                                <p style={{ margin: '6px 0', color: 'black', fontSize: '13px' }}>
                                    <strong>Price:</strong> ₪{selectedSpot.pricePerHour}/hr
                                </p>
                            )}


                            <p style={{ margin: '6px 0', color: 'black', fontSize: '13px' }}>
                                <strong>Rating:</strong>{' '}
                                {selectedSpot.ratingCount > 0
                                    ? `${Number(selectedSpot.averageRating).toFixed(1)} / 5 (${selectedSpot.ratingCount} ratings)`
                                    : 'No ratings yet'}
                            </p>

                            <div style={{ display: 'flex', gap: 10, marginTop: 14, marginBottom: 12 }}>
                                <button
                                    type="button"
                                    onClick={() => handleNavigate(selectedSpot.lat, selectedSpot.lng, 'waze')}
                                    style={btnStyleWaze}
                                    onMouseOver={(e) => e.currentTarget.style.background = '#f1f5f9'}
                                    onMouseOut={(e) => e.currentTarget.style.background = '#f8fafc'}
                                >
                                    Waze
                                </button>
                                <button
                                    type="button"
                                    onClick={() => handleNavigate(selectedSpot.lat, selectedSpot.lng, 'google')}
                                    style={btnStyleGoogle}
                                    onMouseOver={(e) => e.currentTarget.style.background = '#f1f5f9'}
                                    onMouseOut={(e) => e.currentTarget.style.background = '#f8fafc'}
                                >
                                    Maps
                                </button>
                            </div>

                            {onSpotClick && (
                                <div style={{ marginTop: '10px' }}>
                                    <button
                                        type="button"
                                        onClick={() => setDetailModalSpot(selectedSpot)}
                                        style={btnStyleDetails}
                                        onMouseOver={(e) => e.currentTarget.style.background = '#f1f5f9'}
                                        onMouseOut={(e) => e.currentTarget.style.background = '#f8fafc'}
                                    >
                                        View Spot Details
                                    </button>

                                    {isMine ? (
                                        <div style={{
                                            textAlign: 'center',
                                            padding: '10px',
                                            backgroundColor: '#fee2e2',
                                            color: '#b91c1c',
                                            borderRadius: '10px',
                                            fontWeight: '700',
                                            border: '1px solid #fecaca',
                                            fontSize: '13px'
                                        }}>
                                            This parking spot is yours
                                        </div>
                                    ) : (
                                        <button
                                            type="button"
                                            onClick={() => onSpotClick(selectedSpot)}
                                            style={btnStyleRequest}
                                            onMouseOver={(e) => e.currentTarget.style.opacity = '0.9'}
                                            onMouseOut={(e) => e.currentTarget.style.opacity = '1'}
                                            onMouseDown={(e) => e.currentTarget.style.transform = 'scale(0.98)'}
                                            onMouseUp={(e) => e.currentTarget.style.transform = 'scale(1)'}
                                        >
                                            Request booking
                                        </button>
                                    )}
                                </div>
                            )}
                        </div>
                    </InfoWindow>
                )}
            </GoogleMap>

            <SpotDetailModal
                spot={detailModalSpot}
                onClose={() => setDetailModalSpot(null)}
            />
        </div>
    )
}