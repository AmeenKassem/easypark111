import React, { useState } from 'react';
import { Autocomplete } from '@react-google-maps/api';

const AddressAutocomplete = ({ onAddressSelect, options }) => {
  const [autocomplete, setAutocomplete] = useState(null);

  const onLoad = (autocompleteInstance) => {
    setAutocomplete(autocompleteInstance);
  };

  const onPlaceChanged = () => {
    if (autocomplete !== null) {
      const place = autocomplete.getPlace();

      if (!place.geometry || !place.geometry.location) {
        alert("Please select a valid address from the dropdown list.");
        return;
      }

      const lat = place.geometry.location.lat();
      const lng = place.geometry.location.lng();
      const address = place.formatted_address;
      // Extract components to check for street number later
      const address_components = place.address_components;

      // Pass components up to the parent
      onAddressSelect({ lat, lng, address, address_components });
    }
  };

  return (
    <div style={{ marginBottom: '15px' }}>
      <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
        Parking Location
      </label>

      {/* Pass the 'options' prop to Google's Autocomplete */}
      <Autocomplete
        onLoad={onLoad}
        onPlaceChanged={onPlaceChanged}
        options={options}
      >
        <input
          type="text"
          placeholder="Search address (e.g. Herzl 15)..."
          style={{
            width: '100%',
            padding: '12px',
            borderRadius: '8px',
            border: '1px solid #ccc',
            fontSize: '16px'
          }}
        />
      </Autocomplete>
    </div>
  );
};

export default AddressAutocomplete;