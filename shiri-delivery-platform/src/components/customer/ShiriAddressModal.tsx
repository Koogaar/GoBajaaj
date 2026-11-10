import React, { useState, useEffect } from 'react';
import { ShiriAddress, Coordinates } from '../../types';
import { platformStore } from '../../services/store';
import { translations } from '../../i18n/translations';
import { LocationService } from '../../services/locationService';
import { parseLocationInput, buildGoogleMapsViewUrl } from '../../utils/coordinateParser';
import { MapCanvas } from '../common/MapCanvas';
import { QrCodeSvg } from '../common/QrCodeSvg';
import {
  X,
  MapPin,
  Share2,
  Copy,
  Check,
  Navigation,
  QrCode,
  Crosshair,
  ExternalLink,
  Link as LinkIcon,
  AlertCircle,
  Loader2,
  Sliders,
} from 'lucide-react';

interface ShiriAddressModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAddressCreated?: (addr: ShiriAddress) => void;
  initialAddress?: ShiriAddress | null;
}

export const ShiriAddressModal: React.FC<ShiriAddressModalProps> = ({
  isOpen,
  onClose,
  onAddressCreated,
  initialAddress,
}) => {
  const lang = platformStore.getLanguage();
  const t = translations[lang];

  const [activeStep, setActiveStep] = useState<'form' | 'success'>(initialAddress ? 'success' : 'form');
  const [createdAddress, setCreatedAddress] = useState<ShiriAddress | null>(initialAddress || null);

  // Form State
  const [label, setLabel] = useState<'Home' | 'Work' | 'Family' | 'Store'>('Home');
  const [district, setDistrict] = useState<string>('Hodan');
  const [neighborhood, setNeighborhood] = useState<string>('Digfeer');
  const [street, setStreet] = useState<string>('Waddo 21');
  const [buildingOrFloor, setBuildingOrFloor] = useState<string>('Guri 12, Dabaqa 2-aad');
  const [landmark, setLandmark] = useState<string>('Agta Masaajidka Digfeer / Near Digfeer Hospital');
  const [notes, setNotes] = useState<string>('Albaabka buluugga ah, wac markaad soo gaarto');
  const [coords, setCoords] = useState<Coordinates>({ lat: 2.0469, lng: 45.3182 });
  const [isCopied, setIsCopied] = useState<boolean>(false);

  // Location-Sharing & Geolocation State
  const [isLocating, setIsLocating] = useState<boolean>(false);
  const [locationStatus, setLocationStatus] = useState<{
    type: 'success' | 'error' | 'info';
    message: string;
  } | null>(null);
  const [gpsAccuracy, setGpsAccuracy] = useState<number | null>(null);
  const [showCoordinateDrawer, setShowCoordinateDrawer] = useState<boolean>(false);
  const [linkOrCoordInput, setLinkOrCoordInput] = useState<string>('');
  const [linkError, setLinkError] = useState<string | null>(null);
  const [linkSuccess, setLinkSuccess] = useState<string | null>(null);
  const [coordSource, setCoordSource] = useState<'default' | 'gps' | 'link' | 'manual'>('default');

  // Manual Coordinate inputs
  const [latInput, setLatInput] = useState<string>('2.046900');
  const [lngInput, setLngInput] = useState<string>('45.318200');

  useEffect(() => {
    setLatInput(coords.lat.toFixed(6));
    setLngInput(coords.lng.toFixed(6));
  }, [coords.lat, coords.lng]);

  if (!isOpen) return null;

  // Use My Current Location (Browser Geolocation API)
  const handleUseCurrentLocation = () => {
    setIsLocating(true);
    setLocationStatus(null);

    LocationService.getCurrentLocation(
      (newCoords, accuracy) => {
        setIsLocating(false);
        setCoords(newCoords);
        setGpsAccuracy(accuracy);
        setCoordSource('gps');
        setLocationStatus({
          type: 'success',
          message: `GPS Acquired: ${newCoords.lat.toFixed(5)}, ${newCoords.lng.toFixed(5)} (±${accuracy.toFixed(0)}m accuracy)`,
        });
      },
      (status, message) => {
        setIsLocating(false);
        setLocationStatus({
          type: 'error',
          message:
            status === 'denied'
              ? 'Location permission denied. To enable: click the lock/settings icon in your browser address bar and set Location to Allow. You can also manually adjust the map pin or paste coordinates below.'
              : message,
        });
        // Automatically open the coordinate drawer as manual fallback
        setShowCoordinateDrawer(true);
      }
    );
  };

  // Parse pasted Google Maps link or coordinates
  const handleParseInput = (e: React.FormEvent) => {
    e.preventDefault();
    setLinkError(null);
    setLinkSuccess(null);

    const result = parseLocationInput(linkOrCoordInput);
    if (!result.success || !result.coords) {
      setLinkError(result.error || 'Invalid link or coordinates.');
      return;
    }

    setCoords(result.coords);
    setCoordSource(result.sourceType === 'maps_url' ? 'link' : 'manual');
    setLinkSuccess(`Loaded coordinates: ${result.coords.lat.toFixed(6)}, ${result.coords.lng.toFixed(6)}`);
    setLocationStatus({
      type: 'success',
      message: `Location updated from ${result.sourceType === 'maps_url' ? 'Google Maps link' : 'coordinates'}!`,
    });
    setTimeout(() => {
      setShowCoordinateDrawer(false);
      setLinkSuccess(null);
    }, 1200);
  };

  // Manual coordinate input change
  const handleApplyManualCoords = () => {
    const parsedLat = parseFloat(latInput);
    const parsedLng = parseFloat(lngInput);
    if (isNaN(parsedLat) || isNaN(parsedLng) || parsedLat < -90 || parsedLat > 90 || parsedLng < -180 || parsedLng > 180) {
      setLocationStatus({
        type: 'error',
        message: 'Invalid latitude (-90 to 90) or longitude (-180 to 180).',
      });
      return;
    }
    const updated: Coordinates = {
      lat: Number(parsedLat.toFixed(6)),
      lng: Number(parsedLng.toFixed(6)),
    };
    setCoords(updated);
    setCoordSource('manual');
    setLocationStatus({
      type: 'success',
      message: `Updated to ${updated.lat.toFixed(5)}, ${updated.lng.toFixed(5)}`,
    });
  };

  if (!isOpen) return null;

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();

    const newAddr = platformStore.createAddress({
      ownerId: 'cust-1',
      ownerName: 'Ahmed Mohamed',
      ownerPhone: '+252 61 234 5678',
      label,
      labelSomali: label === 'Home' ? 'Gurigayga' : label === 'Work' ? 'Shaqada' : 'Reerka',
      city: 'Mogadishu',
      district,
      neighborhood,
      street,
      buildingOrFloor,
      landmark,
      coordinates: coords,
      accuracyMeters: gpsAccuracy || 8,
      notes,
    });

    setCreatedAddress(newAddr);
    setActiveStep('success');
    if (onAddressCreated) {
      onAddressCreated(newAddr);
    }
  };

  const handleCopyCode = () => {
    if (createdAddress) {
      navigator.clipboard?.writeText(createdAddress.addressCode);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    }
  };

  const googleMapsPinUrl = buildGoogleMapsViewUrl(coords, `${label} - Shiri Delivery`);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
      <div className="bg-white rounded-3xl w-full max-w-md max-h-[92vh] overflow-y-auto shadow-2xl border border-slate-100 flex flex-col">
        {/* Header */}
        <div className="sticky top-0 bg-white/95 backdrop-blur-sm z-20 px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-red-100 text-red-600 flex items-center justify-center">
              <MapPin className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 leading-tight">
                {t.shiriAddressTitle}
              </h2>
              <p className="text-[11px] text-slate-500">
                {t.shiriAddressSubtitle}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 flex items-center justify-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 flex-1">
          {activeStep === 'form' ? (
            <form onSubmit={handleSave} className="space-y-4">
              {/* Location Tools & Real Geolocation Section */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                    <Navigation className="w-3.5 h-3.5 text-red-600" />
                    <span>Goobta & Khariidadda (Location & Pin)</span>
                  </label>
                  <span className="text-[10px] text-slate-500">
                    No API Key required
                  </span>
                </div>

                {/* Primary Action Buttons: Geolocation, Open Google Maps, Paste Link */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={handleUseCurrentLocation}
                    disabled={isLocating}
                    className={`min-h-[42px] py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-xs ${
                      isLocating
                        ? 'bg-red-950 text-red-300 border border-red-800'
                        : 'bg-red-600 hover:bg-red-700 text-white active:scale-95'
                    }`}
                    title="Detect real device GPS position"
                  >
                    {isLocating ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin shrink-0" />
                    ) : (
                      <Crosshair className="w-3.5 h-3.5 shrink-0" />
                    )}
                    <span className="truncate">{isLocating ? 'Raadinaya GPS...' : 'Isticmaal GPS-keyga'}</span>
                  </button>

                  <a
                    href={googleMapsPinUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="min-h-[42px] py-2 px-3 rounded-xl text-xs font-bold bg-slate-900 hover:bg-slate-800 text-emerald-300 border border-slate-700 flex items-center justify-center gap-1.5 transition-all shadow-xs"
                    title="View current pin coordinates in Google Maps"
                  >
                    <ExternalLink className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span className="truncate">Fur Google Maps</span>
                  </a>

                  <button
                    type="button"
                    onClick={() => setShowCoordinateDrawer(!showCoordinateDrawer)}
                    className="min-h-[42px] py-2 px-3 rounded-xl text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-200 flex items-center justify-center gap-1.5 transition-all"
                    title="Paste Google Maps URL or enter raw coordinates"
                  >
                    <LinkIcon className="w-3.5 h-3.5 text-red-600 shrink-0" />
                    <span className="truncate">Geli Link / Coords</span>
                  </button>
                </div>

                {/* Status Message Toast */}
                {locationStatus && (
                  <div
                    className={`p-2.5 rounded-xl text-xs flex items-start gap-2 animate-in fade-in ${
                      locationStatus.type === 'success'
                        ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                        : 'bg-rose-50 text-rose-800 border border-rose-200'
                    }`}
                  >
                    {locationStatus.type === 'success' ? (
                      <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    ) : (
                      <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                    )}
                    <span className="flex-1 leading-snug">{locationStatus.message}</span>
                    <button
                      type="button"
                      onClick={() => setLocationStatus(null)}
                      className="text-slate-400 hover:text-slate-600 text-xs font-bold"
                    >
                      ✕
                    </button>
                  </div>
                )}

                {/* Drawer: Paste Google Maps Link or Enter Coordinates */}
                {showCoordinateDrawer && (
                  <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200 space-y-2 animate-in fade-in">
                    <div className="flex items-center justify-between text-xs font-bold text-slate-800">
                      <span>Share or Enter Google Maps Link / Coordinates</span>
                      <button
                        type="button"
                        onClick={() => setShowCoordinateDrawer(false)}
                        className="text-slate-400 hover:text-slate-600"
                      >
                        ✕
                      </button>
                    </div>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={linkOrCoordInput}
                        onChange={(e) => setLinkOrCoordInput(e.target.value)}
                        placeholder="e.g. 2.0469, 45.3182 or https://maps.google.com/?q=..."
                        className="flex-1 px-3 py-1.5 bg-white border border-slate-300 rounded-xl text-xs font-mono text-slate-800 outline-none focus:border-red-600"
                      />
                      <button
                        type="button"
                        onClick={handleParseInput}
                        className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shrink-0"
                      >
                        Apply
                      </button>
                    </div>

                    {linkError && (
                      <p className="text-[11px] text-rose-600 font-medium">{linkError}</p>
                    )}
                    {linkSuccess && (
                      <p className="text-[11px] text-emerald-600 font-medium">{linkSuccess}</p>
                    )}
                  </div>
                )}

                {/* Map Canvas with Pin & District Guide */}
                <div className="rounded-2xl overflow-hidden border border-slate-200">
                  <MapCanvas
                    mode="picker"
                    height="h-48"
                    selectedCoords={coords}
                    onCoordsChange={(newCoords, addressInfo) => {
                      setCoords(newCoords);
                      setCoordSource('manual');
                      if (addressInfo?.district) setDistrict(addressInfo.district);
                      if (addressInfo?.neighborhood) setNeighborhood(addressInfo.neighborhood);
                      if (addressInfo?.street) setStreet(addressInfo.street);
                      if (addressInfo?.landmark) setLandmark(addressInfo.landmark);
                    }}
                    interactive={true}
                    showMyLocationButton={true}
                  />
                </div>

                {/* Coordinate Verification & Fine-Tune Manual Lat/Lng */}
                <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-200 space-y-2">
                  <div className="flex items-center justify-between text-[11px]">
                    <div className="flex items-center gap-1.5">
                      <span className="font-semibold text-slate-700">Source:</span>
                      <span
                        className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                          coordSource === 'gps'
                            ? 'bg-blue-100 text-blue-800'
                            : coordSource === 'link'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-slate-200 text-slate-700'
                        }`}
                      >
                        {coordSource === 'gps'
                          ? `Real Device GPS (±${gpsAccuracy?.toFixed(0) || 10}m)`
                          : coordSource === 'link'
                          ? 'Google Maps Link'
                          : 'Manual Pin Placement'}
                      </span>
                    </div>

                    <span className="font-mono font-bold text-slate-800 text-[11px]">
                      {coords.lat.toFixed(6)}, {coords.lng.toFixed(6)}
                    </span>
                  </div>

                  {/* Manual Coordinate Adjustment Inputs */}
                  <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-200/80">
                    <div>
                      <span className="text-[10px] text-slate-500 font-medium">Latitude</span>
                      <input
                        type="number"
                        step="0.000001"
                        value={latInput}
                        onChange={(e) => setLatInput(e.target.value)}
                        onBlur={handleApplyManualCoords}
                        className="w-full px-2 py-1 text-xs font-mono bg-white border border-slate-300 rounded-lg outline-none focus:border-red-600"
                      />
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500 font-medium">Longitude</span>
                      <input
                        type="number"
                        step="0.000001"
                        value={lngInput}
                        onChange={(e) => setLngInput(e.target.value)}
                        onBlur={handleApplyManualCoords}
                        className="w-full px-2 py-1 text-xs font-mono bg-white border border-slate-300 rounded-lg outline-none focus:border-red-600"
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* Address Label Choice */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Nooca Ciwaanka (Label)
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {(['Home', 'Work', 'Family', 'Store'] as const).map((l) => (
                    <button
                      key={l}
                      type="button"
                      onClick={() => setLabel(l)}
                      className={`py-2 px-2 text-xs font-semibold rounded-xl border text-center transition-all ${
                        label === l
                          ? 'bg-red-600 text-white border-red-600 shadow-sm'
                          : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                      }`}
                    >
                      {l === 'Home' ? '🏠 Guri' : l === 'Work' ? '💼 Shaqo' : l === 'Family' ? '👨‍👩‍👧 Reer' : '🏪 Dukaan'}
                    </button>
                  ))}
                </div>
              </div>

              {/* District & Neighborhood */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {t.district} *
                  </label>
                  <select
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white font-medium text-slate-800 outline-none focus:border-red-600 focus:ring-1 focus:ring-red-600"
                  >
                    <option value="Hodan">Hodan (Degmada)</option>
                    <option value="Waberi">Waberi (Degmada)</option>
                    <option value="Hamarweyne">Hamarweyne</option>
                    <option value="Wadajir">Wadajir (Medina)</option>
                    <option value="Dayniile">Dayniile</option>
                    <option value="Yaqshid">Yaqshid</option>
                    <option value="Howlwadaag">Howlwadaag</option>
                    <option value="Karaan">Karaan</option>
                    <option value="Shibis">Shibis</option>
                    <option value="Boondheere">Boondheere</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {t.neighborhood} *
                  </label>
                  <input
                    type="text"
                    required
                    value={neighborhood}
                    onChange={(e) => setNeighborhood(e.target.value)}
                    placeholder="Digfeer, Taleex, KM4..."
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white font-medium text-slate-800 outline-none focus:border-red-600 focus:ring-1 focus:ring-red-600"
                  />
                </div>
              </div>

              {/* Street & House Number */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {t.street}
                  </label>
                  <input
                    type="text"
                    value={street}
                    onChange={(e) => setStreet(e.target.value)}
                    placeholder="Waddo 21, Maka Al-Mukarama"
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white font-medium text-slate-800 outline-none focus:border-red-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {t.houseNumber}
                  </label>
                  <input
                    type="text"
                    value={buildingOrFloor}
                    onChange={(e) => setBuildingOrFloor(e.target.value)}
                    placeholder="Guri 12, Dabaqa 2"
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white font-medium text-slate-800 outline-none focus:border-red-600"
                  />
                </div>
              </div>

              {/* Prominent Landmark */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {t.landmark} *
                </label>
                <input
                  type="text"
                  required
                  value={landmark}
                  onChange={(e) => setLandmark(e.target.value)}
                  placeholder="Masaajidka kuugu dhow, isbitaal, dugsi..."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white font-medium text-slate-800 outline-none focus:border-red-600"
                />
              </div>

              {/* Delivery Notes */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Tilmaamaha Darawalka (Notes)
                </label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Albaabka midabkiisa, waxaad dooneyso inuu darawalku ogaado..."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white font-medium text-slate-800 outline-none focus:border-red-600 resize-none"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 px-4 bg-red-600 hover:bg-red-700 active:scale-[0.98] text-white font-bold text-sm rounded-xl shadow-lg shadow-red-600/20 transition-all flex items-center justify-center gap-2"
              >
                <Navigation className="w-4 h-4" />
                <span>{t.saveAddress}</span>
              </button>
            </form>
          ) : (
            /* Success & Digital Address Card */
            createdAddress && (
              <div className="space-y-5 animate-in fade-in">
                <div className="text-center">
                  <div className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-2">
                    <Check className="w-6 h-6" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900">
                    {t.addressSavedSuccess}
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Guri Number Digital ah oo diyaar u ah gaarsiin kasta
                  </p>
                </div>

                {/* Shiri Address Card as shown in design references */}
                <div className="bg-gradient-to-br from-red-600 to-red-700 text-white p-5 rounded-3xl shadow-xl relative overflow-hidden">
                  {/* Decorative background curves */}
                  <div className="absolute -right-8 -bottom-8 w-32 h-32 bg-white/10 rounded-full pointer-events-none" />

                  <div className="flex items-start justify-between">
                    <div>
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-red-200">
                        SHIRI ADDRESS · GURI NUMBER
                      </span>
                      <h4 className="text-2xl font-black font-mono tracking-tight mt-0.5">
                        {createdAddress.addressCode}
                      </h4>
                    </div>
                    <span className="bg-white/20 backdrop-blur-xs text-white text-[11px] font-bold px-2.5 py-1 rounded-full">
                      {createdAddress.label}
                    </span>
                  </div>

                  <div className="mt-4 pt-4 border-t border-red-500/50 space-y-1.5 text-xs">
                    <div className="flex justify-between">
                      <span className="text-red-200">{t.district}:</span>
                      <span className="font-semibold">{createdAddress.district}, {createdAddress.neighborhood}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-red-200">{t.street}:</span>
                      <span className="font-semibold">{createdAddress.street || 'Waddo'} · {createdAddress.buildingOrFloor}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-red-200">Landmark:</span>
                      <span className="font-semibold truncate max-w-[200px]">{createdAddress.landmark}</span>
                    </div>
                    <div className="flex justify-between text-[11px]">
                      <span className="text-red-200">GPS Coords:</span>
                      <span className="font-mono text-white/90">
                        {createdAddress.coordinates.lat.toFixed(4)}, {createdAddress.coordinates.lng.toFixed(4)} (±{createdAddress.accuracyMeters}m)
                      </span>
                    </div>
                  </div>
                </div>

                {/* QR Code section */}
                <div className="flex flex-col items-center">
                  <QrCodeSvg
                    value={createdAddress.qrData}
                    size={160}
                    addressCode={createdAddress.addressCode}
                  />
                  <p className="text-[11px] text-slate-500 mt-2 text-center max-w-xs">
                    {t.qrCodeSubtitle}
                  </p>
                </div>

                {/* Action Buttons */}
                <div className="space-y-2 pt-2">
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={handleCopyCode}
                      className="py-2.5 px-3 bg-slate-100 hover:bg-slate-200 text-slate-800 font-semibold text-xs rounded-xl flex items-center justify-center gap-1.5 transition-colors"
                    >
                      {isCopied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{isCopied ? t.copied : t.copyCode}</span>
                    </button>
                    <button
                      onClick={() => {
                        if (navigator.share) {
                          navigator.share({
                            title: 'Shiri Address',
                            text: `My Shiri Address code: ${createdAddress.addressCode} in ${createdAddress.district}, Mogadishu. GPS: ${createdAddress.coordinates.lat.toFixed(6)}, ${createdAddress.coordinates.lng.toFixed(6)}`,
                          }).catch(() => {});
                        } else {
                          handleCopyCode();
                        }
                      }}
                      className="py-2.5 px-3 bg-red-600 hover:bg-red-700 text-white font-semibold text-xs rounded-xl flex items-center justify-center gap-1.5 shadow-md shadow-red-600/20 transition-colors"
                    >
                      <Share2 className="w-3.5 h-3.5" />
                      <span>{t.shareAddress}</span>
                    </button>
                  </div>

                  <a
                    href={buildGoogleMapsViewUrl(createdAddress.coordinates, `${createdAddress.addressCode} (${createdAddress.label})`)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="w-full py-2.5 px-3 bg-slate-900 hover:bg-slate-800 text-emerald-300 font-bold text-xs rounded-xl flex items-center justify-center gap-1.5 transition-colors border border-slate-700 shadow-xs flex-wrap"
                  >
                    <ExternalLink className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Fur Google Maps</span>
                    <span className="font-mono text-[11px] text-emerald-400/80">
                      ({createdAddress.coordinates.lat.toFixed(4)}, {createdAddress.coordinates.lng.toFixed(4)})
                    </span>
                  </a>
                </div>

                <button
                  onClick={onClose}
                  className="w-full py-2.5 text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors"
                >
                  {t.close}
                </button>
              </div>
            )
          )}
        </div>
      </div>
    </div>
  );
};
