/// <reference types="@types/google.maps" />
import React, { useState, useEffect, useCallback } from 'react';
import {
  APIProvider,
  Map,
  AdvancedMarker,
  useMap,
  useMapsLibrary,
  ControlPosition,
  MapControl,
} from '@vis.gl/react-google-maps';
import { Coordinates } from '../../types';
import { platformStore } from '../../services/store';
import { LocationService } from '../../services/locationService';
import {
  parseLocationInput,
  buildGoogleMapsViewUrl,
  buildGoogleMapsNavigateUrl,
} from '../../utils/coordinateParser';
import {
  MapPin,
  Crosshair,
  ExternalLink,
  Navigation,
  AlertTriangle,
  Key,
  Check,
  Loader2,
  RefreshCw,
  Search,
  Layers,
  Store,
  Bike,
  Link as LinkIcon,
  Compass,
  ArrowUp,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Info,
} from 'lucide-react';

export interface ReverseGeocodeInfo {
  district?: string;
  neighborhood?: string;
  street?: string;
  landmark?: string;
  formattedAddress?: string;
}

export interface MapCanvasProps {
  mode?: 'picker' | 'route' | 'fleet';
  selectedCoords?: Coordinates;
  onCoordsChange?: (coords: Coordinates, addressInfo?: ReverseGeocodeInfo) => void;
  pickupCoords?: Coordinates;
  dropoffCoords?: Coordinates;
  courierCoords?: Coordinates;
  courierLabel?: string;
  pickupLabel?: string;
  dropoffLabel?: string;
  className?: string;
  height?: string;
  interactive?: boolean;
  showMyLocationButton?: boolean;
}

// Helper to determine general Mogadishu district from coordinates if geocoder is offline
function estimateMogadishuDistrict(coords: Coordinates): string {
  // Rough bounding boxes for Mogadishu districts
  if (coords.lat >= 2.04 && coords.lat <= 2.055 && coords.lng >= 45.31 && coords.lng <= 45.328) {
    return 'Hodan (Digfeer / Taleex)';
  }
  if (coords.lat >= 2.025 && coords.lat <= 2.038 && coords.lng >= 45.325 && coords.lng <= 45.34) {
    return 'Waberi (KM4 / Maka Al-Mukarama)';
  }
  if (coords.lat >= 2.032 && coords.lat <= 2.045 && coords.lng >= 45.335 && coords.lng <= 45.35) {
    return 'Hamarweyne (Old Town / Coast)';
  }
  if (coords.lat >= 2.015 && coords.lat <= 2.028 && coords.lng >= 45.295 && coords.lng <= 45.315) {
    return 'Wadajir (Medina / Airport Area)';
  }
  if (coords.lat >= 2.058 && coords.lat <= 2.075 && coords.lng >= 45.285 && coords.lng <= 45.305) {
    return 'Dayniile';
  }
  if (coords.lat >= 2.055 && coords.lat <= 2.075 && coords.lng >= 45.34 && coords.lng <= 45.36) {
    return 'Yaqshid';
  }
  if (coords.lat >= 1.9 && coords.lat <= 2.2 && coords.lng >= 45.1 && coords.lng <= 45.5) {
    return 'Mogadishu';
  }
  return `GPS (${coords.lat.toFixed(4)}, ${coords.lng.toFixed(4)})`;
}

// Inner Controller for when Google Maps SDK is active
const GoogleMapController: React.FC<{
  mode: 'picker' | 'route' | 'fleet';
  pinPosition: Coordinates;
  setPinPosition: React.Dispatch<React.SetStateAction<Coordinates>>;
  onCoordsChange?: (coords: Coordinates, addressInfo?: ReverseGeocodeInfo) => void;
  pickupCoords: Coordinates;
  dropoffCoords: Coordinates;
  courierCoords?: Coordinates;
  courierLabel: string;
  pickupLabel: string;
  dropoffLabel: string;
  interactive: boolean;
  deviceGpsCoords: Coordinates | null;
  onRequestMyLocation: () => void;
  isLocating: boolean;
  mapType: 'roadmap' | 'satellite' | 'hybrid';
  setMapType: (t: 'roadmap' | 'satellite' | 'hybrid') => void;
}> = ({
  mode,
  pinPosition,
  setPinPosition,
  onCoordsChange,
  pickupCoords,
  dropoffCoords,
  courierCoords,
  courierLabel,
  pickupLabel,
  dropoffLabel,
  interactive,
  deviceGpsCoords,
  onRequestMyLocation,
  isLocating,
  mapType,
  setMapType,
}) => {
  const map = useMap();
  const geocodingLib = useMapsLibrary('geocoding');
  const [geocoder, setGeocoder] = useState<google.maps.Geocoder | null>(null);

  useEffect(() => {
    if (geocodingLib && !geocoder) {
      setGeocoder(new geocodingLib.Geocoder());
    }
  }, [geocodingLib, geocoder]);

  const reverseGeocode = useCallback(
    (coords: Coordinates) => {
      if (!geocoder) {
        if (onCoordsChange) onCoordsChange(coords, { district: estimateMogadishuDistrict(coords) });
        return;
      }

      geocoder.geocode(
        { location: { lat: coords.lat, lng: coords.lng } },
        (results: google.maps.GeocoderResult[] | null, status: string) => {
          let info: ReverseGeocodeInfo = {};
          if (status === 'OK' && results && results[0]) {
            const res = results[0];
            info.formattedAddress = res.formatted_address;

            for (const comp of res.address_components) {
              const types = comp.types;
              if (
                types.includes('sublocality') ||
                types.includes('sublocality_level_1') ||
                types.includes('administrative_area_level_2')
              ) {
                info.district = comp.long_name;
              } else if (types.includes('neighborhood')) {
                info.neighborhood = comp.long_name;
              } else if (types.includes('route')) {
                info.street = comp.long_name;
              } else if (types.includes('point_of_interest') || types.includes('establishment')) {
                info.landmark = comp.long_name;
              }
            }
          } else {
            info.district = estimateMogadishuDistrict(coords);
          }
          if (onCoordsChange) {
            onCoordsChange(coords, info);
          }
        }
      );
    },
    [geocoder, onCoordsChange]
  );

  useEffect(() => {
    if (map && pinPosition) {
      map.panTo(pinPosition);
    }
  }, [map, pinPosition]);

  const handleDragEnd = (e: google.maps.MapMouseEvent) => {
    if (!e.latLng) return;
    const newCoords: Coordinates = {
      lat: Number(e.latLng.lat().toFixed(6)),
      lng: Number(e.latLng.lng().toFixed(6)),
    };
    setPinPosition(newCoords);
    reverseGeocode(newCoords);
  };

  return (
    <>
      <MapControl position={ControlPosition.TOP_RIGHT}>
        <div className="flex flex-col gap-1.5 p-2 pointer-events-auto">
          <button
            type="button"
            onClick={() => setMapType(mapType === 'roadmap' ? 'hybrid' : 'roadmap')}
            className="w-9 h-9 rounded-xl bg-white/95 backdrop-blur-sm text-slate-700 shadow-md border border-slate-200 flex items-center justify-center hover:bg-slate-50 transition-colors"
            title="Toggle Satellite / Streets"
          >
            <Layers className="w-4 h-4 text-slate-700" />
          </button>

          <button
            type="button"
            onClick={onRequestMyLocation}
            disabled={isLocating}
            className={`w-9 h-9 rounded-xl shadow-md border flex items-center justify-center transition-all ${
              isLocating
                ? 'bg-red-50 border-red-300 text-red-600'
                : 'bg-white/95 backdrop-blur-sm border-slate-200 text-slate-700 hover:bg-slate-50'
            }`}
            title="Use My Current Location (GPS)"
          >
            {isLocating ? (
              <Loader2 className="w-4 h-4 text-red-600 animate-spin" />
            ) : (
              <Crosshair className="w-4 h-4 text-red-600" />
            )}
          </button>
        </div>
      </MapControl>

      {/* User GPS location beacon */}
      {deviceGpsCoords && (
        <AdvancedMarker position={deviceGpsCoords} title="My Current Device GPS Location">
          <div className="relative flex items-center justify-center">
            <div className="w-4 h-4 rounded-full bg-blue-600 border-2 border-white shadow-md z-10" />
            <div className="absolute w-8 h-8 rounded-full bg-blue-400/40 animate-ping -z-10" />
          </div>
        </AdvancedMarker>
      )}

      {/* Draggable Shiri Delivery Pin */}
      {mode === 'picker' && (
        <AdvancedMarker
          position={pinPosition}
          draggable={interactive}
          onDragEnd={handleDragEnd}
          title="Shiri Delivery Pin (Drag to entrance)"
        >
          <div className="flex flex-col items-center -translate-y-8 cursor-grab active:cursor-grabbing select-none">
            <div className="bg-slate-900 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-lg whitespace-nowrap mb-1">
              Delivery Pin (Drag to door)
            </div>
            <div className="relative">
              <div className="w-9 h-9 rounded-full bg-red-600 border-2 border-white shadow-xl flex items-center justify-center text-white">
                <MapPin className="w-5 h-5 fill-white" />
              </div>
              <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1.5 h-1.5 bg-red-700 rounded-full" />
            </div>
          </div>
        </AdvancedMarker>
      )}

      {/* Route & Fleet markers */}
      {(mode === 'route' || mode === 'fleet') && pickupCoords && (
        <AdvancedMarker position={pickupCoords} title={pickupLabel}>
          <div className="flex flex-col items-center -translate-y-6">
            <div className="bg-amber-600 text-white text-[9px] font-bold px-1.5 py-0.5 rounded shadow whitespace-nowrap mb-0.5">
              🏪 {pickupLabel}
            </div>
            <div className="w-7 h-7 rounded-full bg-amber-500 border-2 border-white shadow flex items-center justify-center text-white text-xs">
              <Store className="w-3.5 h-3.5" />
            </div>
          </div>
        </AdvancedMarker>
      )}

      {(mode === 'route' || mode === 'fleet') && dropoffCoords && (
        <AdvancedMarker position={dropoffCoords} title={dropoffLabel}>
          <div className="flex flex-col items-center -translate-y-6">
            <div className="bg-red-600 text-white text-[9px] font-bold px-1.5 py-0.5 rounded shadow whitespace-nowrap mb-0.5">
              📍 {dropoffLabel}
            </div>
            <div className="w-7 h-7 rounded-full bg-red-600 border-2 border-white shadow flex items-center justify-center text-white text-xs">
              <MapPin className="w-3.5 h-3.5 fill-white" />
            </div>
          </div>
        </AdvancedMarker>
      )}

      {(mode === 'route' || mode === 'fleet') && courierCoords && (
        <AdvancedMarker position={courierCoords} title={courierLabel}>
          <div className="flex flex-col items-center -translate-y-6">
            <div className="bg-slate-900 text-white text-[9px] font-medium px-2 py-0.5 rounded-full shadow-lg whitespace-nowrap mb-0.5 border border-slate-700">
              {courierLabel}
            </div>
            <div className="relative">
              <div className="w-8 h-8 rounded-full bg-emerald-600 border-2 border-white shadow-xl flex items-center justify-center text-white text-xs">
                <Bike className="w-4 h-4" />
              </div>
              <div className="absolute inset-0 rounded-full bg-emerald-500 animate-ping opacity-30" />
            </div>
          </div>
        </AdvancedMarker>
      )}
    </>
  );
};

export const MapCanvas: React.FC<MapCanvasProps> = ({
  mode = 'picker',
  selectedCoords = { lat: 2.0469, lng: 45.3182 },
  onCoordsChange,
  pickupCoords = { lat: 2.0435, lng: 45.3211 },
  dropoffCoords = { lat: 2.0469, lng: 45.3182 },
  courierCoords,
  courierLabel = 'Ahmed Hassan (Bajaj)',
  pickupLabel = 'Pizza House',
  dropoffLabel = 'SHR-MG-48291',
  className = '',
  height = 'h-72',
  interactive = true,
  showMyLocationButton = true,
}) => {
  // Current Pin position
  const [pinPosition, setPinPosition] = useState<Coordinates>(selectedCoords);
  const [mapType, setMapType] = useState<'roadmap' | 'satellite' | 'hybrid'>('roadmap');

  // Device GPS State
  const [deviceGpsCoords, setDeviceGpsCoords] = useState<Coordinates | null>(null);
  const [deviceGpsAccuracy, setDeviceGpsAccuracy] = useState<number | null>(null);
  const [isLocating, setIsLocating] = useState<boolean>(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [locationSuccessMsg, setLocationSuccessMsg] = useState<string | null>(null);
  const [coordSource, setCoordSource] = useState<'default' | 'device_gps' | 'link_parsed' | 'manual'>(
    'default'
  );

  // Link / Coords Input Drawer State
  const [isLinkInputOpen, setIsLinkInputOpen] = useState<boolean>(false);
  const [linkInputValue, setLinkInputValue] = useState<string>('');
  const [parseError, setParseError] = useState<string | null>(null);
  const [parseSuccessMsg, setParseSuccessMsg] = useState<string | null>(null);

  // API Key State
  const [apiKey, setApiKey] = useState<string>(platformStore.getGoogleMapsApiKey());
  const [isKeyDialogOpen, setIsKeyDialogOpen] = useState<boolean>(false);
  const [tempKeyInput, setTempKeyInput] = useState<string>('');

  useEffect(() => {
    if (selectedCoords) {
      setPinPosition(selectedCoords);
    }
  }, [selectedCoords.lat, selectedCoords.lng]);

  useEffect(() => {
    const unsub = platformStore.subscribe(() => {
      setApiKey(platformStore.getGoogleMapsApiKey());
    });
    return unsub;
  }, []);

  // Request Real Device GPS Location using Browser Geolocation
  const handleRequestMyLocation = () => {
    setIsLocating(true);
    setLocationError(null);
    setLocationSuccessMsg(null);

    LocationService.getCurrentLocation(
      (coords, accuracy) => {
        setIsLocating(false);
        setDeviceGpsCoords(coords);
        setDeviceGpsAccuracy(accuracy);
        setCoordSource('device_gps');
        setLocationSuccessMsg(
          `Real device GPS acquired (Lat: ${coords.lat.toFixed(5)}, Lng: ${coords.lng.toFixed(5)} · Accuracy ±${accuracy.toFixed(0)}m)`
        );

        if (mode === 'picker') {
          setPinPosition(coords);
          if (onCoordsChange) {
            onCoordsChange(coords, {
              district: estimateMogadishuDistrict(coords),
            });
          }
        }
      },
      (status, message) => {
        setIsLocating(false);
        setLocationError(
          status === 'denied'
            ? 'Location permission denied. Click the lock/settings icon in the URL bar to allow Location, or use the nudge buttons / Paste Coords below.'
            : message
        );
        if (status === 'denied') {
          setIsLinkInputOpen(true);
        }
      }
    );
  };

  // Parse pasted Google Maps link or latitude/longitude coordinates
  const handleParseLinkSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setParseError(null);
    setParseSuccessMsg(null);

    const result = parseLocationInput(linkInputValue);
    if (!result.success || !result.coords) {
      setParseError(result.error || 'Invalid link or coordinates.');
      return;
    }

    const { coords, sourceType } = result;
    setPinPosition(coords);
    setCoordSource(sourceType === 'maps_url' ? 'link_parsed' : 'manual');
    setParseSuccessMsg(
      `Successfully loaded coordinates: ${coords.lat.toFixed(5)}, ${coords.lng.toFixed(5)}`
    );

    if (onCoordsChange) {
      onCoordsChange(coords, {
        district: estimateMogadishuDistrict(coords),
      });
    }

    setTimeout(() => {
      setIsLinkInputOpen(false);
      setParseSuccessMsg(null);
    }, 1500);
  };

  // Fine-tune manual coordinate nudges (e.g. ±0.0005° is roughly ~55 meters)
  const handleNudge = (deltaLat: number, deltaLng: number) => {
    const updated: Coordinates = {
      lat: Number((pinPosition.lat + deltaLat).toFixed(6)),
      lng: Number((pinPosition.lng + deltaLng).toFixed(6)),
    };
    setPinPosition(updated);
    setCoordSource('manual');
    if (onCoordsChange) {
      onCoordsChange(updated, { district: estimateMogadishuDistrict(updated) });
    }
  };

  // Google Maps Universal Link URLs
  const universalViewUrl = buildGoogleMapsViewUrl(
    mode === 'picker' ? pinPosition : dropoffCoords,
    mode === 'picker' ? 'Shiri Delivery Pin' : dropoffLabel
  );

  const universalNavigateUrl = buildGoogleMapsNavigateUrl(
    mode === 'picker' ? pinPosition : dropoffCoords
  );

  const hasValidApiKey = Boolean(apiKey && apiKey.trim() && apiKey !== 'YOUR_GOOGLE_MAPS_API_KEY');

  return (
    <div
      className={`relative w-full ${height} overflow-hidden rounded-2xl bg-slate-900 border border-slate-700/80 select-none shadow-md flex flex-col ${className}`}
    >
      {/* 1. If Google Maps API Key is active: Render official Google Maps JS SDK */}
      {hasValidApiKey ? (
        <APIProvider
          apiKey={apiKey}
          libraries={['places', 'geocoding', 'routes', 'core', 'maps']}
          language="so"
          region="SO"
        >
          <Map
            mapId="SHIRI_MOGADISHU_MAP"
            defaultCenter={pinPosition || { lat: 2.0469, lng: 45.3182 }}
            defaultZoom={15}
            mapTypeId={mapType}
            gestureHandling={interactive ? 'greedy' : 'none'}
            disableDefaultUI={true}
            style={{ width: '100%', height: '100%' }}
            internalUsageAttributionIds={['gmp_mcp_codeassist_v1_aistudio']}
          >
            <GoogleMapController
              mode={mode}
              pinPosition={pinPosition}
              setPinPosition={setPinPosition}
              onCoordsChange={onCoordsChange}
              pickupCoords={pickupCoords}
              dropoffCoords={dropoffCoords}
              courierCoords={courierCoords}
              courierLabel={courierLabel}
              pickupLabel={pickupLabel}
              dropoffLabel={dropoffLabel}
              interactive={interactive}
              deviceGpsCoords={deviceGpsCoords}
              onRequestMyLocation={handleRequestMyLocation}
              isLocating={isLocating}
              mapType={mapType}
              setMapType={setMapType}
            />
          </Map>
        </APIProvider>
      ) : (
        /* 2. Alternative Real Location Workflow (NO API Key required!) */
        <div className="relative w-full h-full flex flex-col justify-between p-3.5 bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 text-white overflow-hidden">
          {/* Subtle geographic grid pattern of Mogadishu */}
          <div className="absolute inset-0 opacity-15 pointer-events-none bg-[radial-gradient(#ffffff_1.2px,transparent_1.2px)] [background-size:20px_20px]" />

          {/* District reference rings / visual zones */}
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none opacity-20">
            <div className="w-64 h-64 border border-dashed border-red-500 rounded-full" />
            <div className="absolute w-44 h-44 border border-slate-500 rounded-full" />
          </div>

          {/* Top Bar Controls */}
          <div className="relative z-20 flex items-center justify-between gap-2 flex-wrap">
            <div className="flex items-center gap-1.5 bg-slate-900/90 backdrop-blur-md px-2.5 py-1 rounded-xl border border-slate-700/80 text-[11px]">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span className="font-semibold text-slate-200">
                {estimateMogadishuDistrict(pinPosition)}
              </span>
            </div>

            {/* Top Action Buttons: GPS, Link/Coords, Open Google Maps */}
            <div className="flex items-center gap-1.5 flex-wrap">
              {/* Button: Use My Current Location */}
              {showMyLocationButton && (
                <button
                  type="button"
                  onClick={handleRequestMyLocation}
                  disabled={isLocating}
                  className={`px-2.5 py-1.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all shadow-sm ${
                    isLocating
                      ? 'bg-red-950 text-red-300 border border-red-800'
                      : 'bg-red-600 hover:bg-red-700 text-white active:scale-95'
                  }`}
                  title="Detect Device Real GPS Position"
                >
                  {isLocating ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin shrink-0" />
                  ) : (
                    <Crosshair className="w-3.5 h-3.5 shrink-0" />
                  )}
                  <span>{isLocating ? 'GPS...' : 'GPS'}</span>
                </button>
              )}

              {/* Button: Paste Google Maps Link or Coords */}
              {interactive && mode === 'picker' && (
                <button
                  type="button"
                  onClick={() => setIsLinkInputOpen(!isLinkInputOpen)}
                  className="px-2.5 py-1.5 rounded-xl font-semibold text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 flex items-center gap-1 transition-colors"
                  title="Share or Enter Google Maps Link / Coordinates"
                >
                  <LinkIcon className="w-3.5 h-3.5 text-red-400 shrink-0" />
                  <span>Link / Coords</span>
                </button>
              )}

              {/* Button: Open Google Maps (Universal URL) */}
              <a
                href={mode === 'route' ? universalNavigateUrl : universalViewUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="px-2.5 py-1.5 rounded-xl font-semibold text-xs bg-slate-800 hover:bg-slate-700 text-emerald-300 border border-slate-700 flex items-center gap-1 transition-colors shadow-xs"
                title="Open this location in Google Maps app or browser"
              >
                <ExternalLink className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                <span>Maps</span>
              </a>
            </div>
          </div>

          {/* Central Interactive Pin Canvas */}
          <div className="relative z-10 flex-1 flex flex-col items-center justify-center my-2 select-none">
            {/* Center Targeted Pin */}
            <div className="relative flex flex-col items-center">
              <div className="bg-slate-950 text-white text-[11px] font-mono font-bold px-2.5 py-0.5 rounded-full shadow-lg border border-slate-700 mb-1 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-red-600 shrink-0" />
                <span>
                  {pinPosition.lat.toFixed(5)}, {pinPosition.lng.toFixed(5)}
                </span>
              </div>

              <div className="relative">
                <div className="w-10 h-10 rounded-full bg-red-600 border-2 border-white shadow-2xl flex items-center justify-center text-white">
                  <MapPin className="w-5 h-5 fill-white" />
                </div>
                {/* Visual pulse ring */}
                <div className="absolute inset-0 rounded-full bg-red-500/40 animate-ping -z-10" />
              </div>
              <div className="text-[10px] text-slate-400 font-medium mt-1">
                {mode === 'picker' ? 'Confirmed Delivery Point' : dropoffLabel}
              </div>
            </div>

            {/* Fine-Tuning Nudge Controls for Doorway Pin (when interactive) */}
            {interactive && mode === 'picker' && (
              <div className="absolute right-2 top-1/2 -translate-y-1/2 flex flex-col items-center gap-1 bg-slate-950/80 backdrop-blur-md p-1.5 rounded-2xl border border-slate-800 shadow-md">
                <span className="text-[9px] text-slate-400 font-bold mb-0.5 uppercase tracking-wider">
                  Nudge
                </span>
                <button
                  type="button"
                  onClick={() => handleNudge(0.0003, 0)}
                  className="w-7 h-7 bg-slate-800 hover:bg-slate-700 active:scale-95 rounded-lg flex items-center justify-center text-slate-200 transition-colors"
                  title="Nudge North (~30m)"
                >
                  <ArrowUp className="w-3.5 h-3.5" />
                </button>
                <div className="flex gap-1">
                  <button
                    type="button"
                    onClick={() => handleNudge(0, -0.0003)}
                    className="w-7 h-7 bg-slate-800 hover:bg-slate-700 active:scale-95 rounded-lg flex items-center justify-center text-slate-200 transition-colors"
                    title="Nudge West (~30m)"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleNudge(0, 0.0003)}
                    className="w-7 h-7 bg-slate-800 hover:bg-slate-700 active:scale-95 rounded-lg flex items-center justify-center text-slate-200 transition-colors"
                    title="Nudge East (~30m)"
                  >
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
                <button
                  type="button"
                  onClick={() => handleNudge(-0.0003, 0)}
                  className="w-7 h-7 bg-slate-800 hover:bg-slate-700 active:scale-95 rounded-lg flex items-center justify-center text-slate-200 transition-colors"
                  title="Nudge South (~30m)"
                >
                  <ArrowDown className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </div>

          {/* Bottom Information Strip & Coordinate Source Indicator */}
          <div className="relative z-20 flex flex-col sm:flex-row sm:items-center justify-between gap-1.5 text-[11px] pt-1.5 border-t border-slate-800">
            <div className="flex items-center gap-1.5 text-slate-400 min-w-0">
              <span className="text-slate-500 shrink-0">Source:</span>
              <span
                className={`px-2 py-0.5 rounded-md font-semibold text-[10px] truncate ${
                  coordSource === 'device_gps'
                    ? 'bg-blue-950 text-blue-300 border border-blue-800'
                    : coordSource === 'link_parsed'
                    ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                    : 'bg-slate-800 text-slate-300'
                }`}
              >
                {coordSource === 'device_gps'
                  ? `Device GPS (±${deviceGpsAccuracy?.toFixed(0) || 10}m)`
                  : coordSource === 'link_parsed'
                  ? 'Google Maps Link'
                  : 'Manual Coordinate Pin'}
              </span>
            </div>

            {/* Optional Embedded API Key Switcher trigger */}
            <button
              type="button"
              onClick={() => setIsKeyDialogOpen(true)}
              className="text-[10px] text-slate-400 hover:text-slate-200 underline font-medium text-left sm:text-right shrink-0"
            >
              Google Maps API Key
            </button>
          </div>
        </div>
      )}

      {/* Slide-Down Link/Coordinates Parser Sheet */}
      {isLinkInputOpen && (
        <div className="absolute inset-x-3 top-12 z-30 bg-slate-950/95 backdrop-blur-md p-3.5 rounded-2xl border border-slate-700 shadow-2xl animate-in fade-in space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-white">
            <span className="flex items-center gap-1.5">
              <LinkIcon className="w-3.5 h-3.5 text-red-400" />
              <span>Paste Google Maps Link or Coordinates</span>
            </span>
            <button
              type="button"
              onClick={() => setIsLinkInputOpen(false)}
              className="text-slate-400 hover:text-white"
            >
              ✕
            </button>
          </div>

          <form onSubmit={handleParseLinkSubmit} className="space-y-2 text-xs">
            <input
              type="text"
              value={linkInputValue}
              onChange={(e) => setLinkInputValue(e.target.value)}
              placeholder="e.g. 2.0469, 45.3182 or https://maps.google.com/?q=..."
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-white placeholder:text-slate-500 rounded-xl outline-none focus:border-red-500 font-mono text-[11px]"
            />

            {parseError && (
              <p className="text-[11px] text-rose-400 flex items-center gap-1 font-medium">
                <AlertTriangle className="w-3 h-3 shrink-0" />
                <span>{parseError}</span>
              </p>
            )}

            {parseSuccessMsg && (
              <p className="text-[11px] text-emerald-400 flex items-center gap-1 font-medium">
                <Check className="w-3 h-3 shrink-0" />
                <span>{parseSuccessMsg}</span>
              </p>
            )}

            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={() => setIsLinkInputOpen(false)}
                className="px-3 py-1.5 text-slate-400 hover:text-slate-200 font-semibold"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-xs transition-colors"
              >
                Apply Coordinates
              </button>
            </div>
          </form>
        </div>
      )}

      {/* GPS Success Toast */}
      {locationSuccessMsg && (
        <div className="absolute top-2 left-2 right-2 z-30 bg-emerald-700/95 backdrop-blur-md text-white px-3 py-1.5 rounded-xl text-xs font-semibold shadow-lg flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-1.5">
            <Check className="w-3.5 h-3.5 shrink-0" />
            <span className="truncate">{locationSuccessMsg}</span>
          </div>
          <button
            type="button"
            onClick={() => setLocationSuccessMsg(null)}
            className="text-white/80 hover:text-white font-bold ml-2 text-xs"
          >
            ✕
          </button>
        </div>
      )}

      {/* GPS Error Alert */}
      {locationError && (
        <div className="absolute top-2 left-2 right-2 z-30 bg-rose-600/95 backdrop-blur-md text-white px-3 py-2 rounded-xl text-xs font-semibold shadow-lg flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span className="leading-tight">{locationError}</span>
          </div>
          <button
            type="button"
            onClick={() => setLocationError(null)}
            className="text-white/80 hover:text-white font-bold ml-2 text-xs"
          >
            ✕
          </button>
        </div>
      )}

      {/* Optional Google Maps API Key Setup Dialog */}
      {isKeyDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/70 backdrop-blur-xs text-slate-900">
          <div className="bg-white rounded-3xl w-full max-w-sm p-5 shadow-2xl space-y-3.5 border border-slate-100">
            <div className="flex items-center justify-between pb-2 border-b">
              <div className="flex items-center gap-2 text-slate-900 font-bold text-sm">
                <Key className="w-4 h-4 text-red-600" />
                <span>Google Maps API Key (Optional)</span>
              </div>
              <button
                type="button"
                onClick={() => setIsKeyDialogOpen(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <p className="text-[11px] text-slate-600 leading-relaxed">
              Google Maps API Key is <b>not required</b> to use Shiri Delivery. The browser's native GPS and universal Google Maps sharing are fully operational. If you have an API key and wish to embed official Google Maps tiles:
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                platformStore.setGoogleMapsApiKey(tempKeyInput);
                setApiKey(tempKeyInput.trim());
                setIsKeyDialogOpen(false);
              }}
              className="space-y-3 text-xs"
            >
              <input
                type="text"
                value={tempKeyInput}
                onChange={(e) => setTempKeyInput(e.target.value)}
                placeholder="AIzaSy..."
                className="w-full px-3 py-2 text-xs font-mono rounded-xl border border-slate-300 outline-none focus:border-red-600"
              />

              <div className="flex gap-2 pt-1">
                <button
                  type="submit"
                  className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-xs transition-colors"
                >
                  Save Key
                </button>
                <button
                  type="button"
                  onClick={() => setIsKeyDialogOpen(false)}
                  className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-colors"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
