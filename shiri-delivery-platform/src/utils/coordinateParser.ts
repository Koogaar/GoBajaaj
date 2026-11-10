import { Coordinates } from '../types';

export interface ParseResult {
  success: boolean;
  coords?: Coordinates;
  error?: string;
  sourceType?: 'raw_coordinates' | 'maps_url' | 'search_query';
  rawInput?: string;
}

/**
 * Safely parses and validates coordinates from:
 * 1. Raw lat, lng strings: "2.0469, 45.3182" or "2.0469 45.3182"
 * 2. Google Maps URLs:
 *    - https://www.google.com/maps?q=2.0469,45.3182
 *    - https://maps.google.com/?q=2.0469,45.3182
 *    - https://www.google.com/maps/search/?api=1&query=2.0469,45.3182
 *    - https://www.google.com/maps/place/.../@2.046923,45.318212,17z/...
 *    - https://maps.app.goo.gl/... (if coords embedded in URL)
 * 3. Decimal degrees with cardinal directions: "2.0469 N, 45.3182 E"
 */
export function parseLocationInput(input: string): ParseResult {
  const trimmed = input.trim();
  if (!trimmed) {
    return { success: false, error: 'Please enter coordinates or a Google Maps link.' };
  }

  // 1. Try URL patterns
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    try {
      // Pattern A: @lat,lng in URL path e.g. /@2.046923,45.318212,17z
      const atMatch = trimmed.match(/@(-?\d+\.\d+),(-?\d+\.\d+)/);
      if (atMatch) {
        const lat = parseFloat(atMatch[1]);
        const lng = parseFloat(atMatch[2]);
        if (isValidCoordinate(lat, lng)) {
          return {
            success: true,
            coords: { lat: Number(lat.toFixed(6)), lng: Number(lng.toFixed(6)) },
            sourceType: 'maps_url',
            rawInput: trimmed,
          };
        }
      }

      // Pattern B: ?q=lat,lng or &query=lat,lng or ?ll=lat,lng or destination=lat,lng or daddr=lat,lng
      const url = new URL(trimmed);
      const queryParam =
        url.searchParams.get('q') ||
        url.searchParams.get('query') ||
        url.searchParams.get('ll') ||
        url.searchParams.get('destination') ||
        url.searchParams.get('daddr');

      if (queryParam) {
        const queryMatch = queryParam.match(/(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)/);
        if (queryMatch) {
          const lat = parseFloat(queryMatch[1]);
          const lng = parseFloat(queryMatch[2]);
          if (isValidCoordinate(lat, lng)) {
            return {
              success: true,
              coords: { lat: Number(lat.toFixed(6)), lng: Number(lng.toFixed(6)) },
              sourceType: 'maps_url',
              rawInput: trimmed,
            };
          }
        }
      }

      // Pattern C: !3d<lat>!4d<lng> parameter in Google Maps share links
      const dataMatch = trimmed.match(/!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)/);
      if (dataMatch) {
        const lat = parseFloat(dataMatch[1]);
        const lng = parseFloat(dataMatch[2]);
        if (isValidCoordinate(lat, lng)) {
          return {
            success: true,
            coords: { lat: Number(lat.toFixed(6)), lng: Number(lng.toFixed(6)) },
            sourceType: 'maps_url',
            rawInput: trimmed,
          };
        }
      }

      // Pattern D: /place/lat,lng or /dir/.../lat,lng
      const pathMatch = trimmed.match(/(-?\d+\.\d{3,})\s*,\s*(-?\d+\.\d{3,})/);
      if (pathMatch) {
        const lat = parseFloat(pathMatch[1]);
        const lng = parseFloat(pathMatch[2]);
        if (isValidCoordinate(lat, lng)) {
          return {
            success: true,
            coords: { lat: Number(lat.toFixed(6)), lng: Number(lng.toFixed(6)) },
            sourceType: 'maps_url',
            rawInput: trimmed,
          };
        }
      }

      return {
        success: false,
        error:
          'Could not extract coordinates from this link. Please copy the link with coordinates or paste raw coordinates (e.g. 2.0469, 45.3182).',
      };
    } catch {
      // Fall through to regex coordinate check
    }
  }

  // 2. Try raw coordinates format: "lat, lng" or "lat lng" or "lat; lng"
  // Handles positive/negative, optional commas, spaces
  const coordRegex = /^(-?\d{1,2}(?:\.\d+)?)[,\s;]+(-?\d{1,3}(?:\.\d+)?)$/;
  const match = trimmed.match(coordRegex);

  if (match) {
    const lat = parseFloat(match[1]);
    const lng = parseFloat(match[2]);

    if (!isValidCoordinate(lat, lng)) {
      return {
        success: false,
        error: `Coordinates out of range. Latitude must be between -90 and 90, Longitude between -180 and 180. Received: (${lat}, ${lng})`,
      };
    }

    return {
      success: true,
      coords: { lat: Number(lat.toFixed(6)), lng: Number(lng.toFixed(6)) },
      sourceType: 'raw_coordinates',
      rawInput: trimmed,
    };
  }

  // 3. Try with Cardinal letters: "2.0469 N, 45.3182 E"
  const cardinalRegex = /(\d+(?:\.\d+)?)\s*°?\s*([NSns])[,\s]+(\d+(?:\.\d+)?)\s*°?\s*([EWew])/;
  const cardMatch = trimmed.match(cardinalRegex);
  if (cardMatch) {
    let lat = parseFloat(cardMatch[1]);
    if (cardMatch[2].toUpperCase() === 'S') lat = -lat;
    let lng = parseFloat(cardMatch[3]);
    if (cardMatch[4].toUpperCase() === 'W') lng = -lng;

    if (isValidCoordinate(lat, lng)) {
      return {
        success: true,
        coords: { lat: Number(lat.toFixed(6)), lng: Number(lng.toFixed(6)) },
        sourceType: 'raw_coordinates',
        rawInput: trimmed,
      };
    }
  }

  return {
    success: false,
    error:
      'Invalid format. Please enter coordinates like "2.0469, 45.3182" or paste a Google Maps location link.',
  };
}

export function isValidCoordinate(lat: number, lng: number): boolean {
  return !isNaN(lat) && !isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
}

/**
 * Builds universal Google Maps search/view URL
 */
export function buildGoogleMapsViewUrl(coords: Coordinates, label?: string): string {
  const query = label ? `${coords.lat},${coords.lng} (${encodeURIComponent(label)})` : `${coords.lat},${coords.lng}`;
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}

/**
 * Builds universal Google Maps turn-by-turn navigation URL for couriers
 */
export function buildGoogleMapsNavigateUrl(coords: Coordinates): string {
  return `https://www.google.com/maps/dir/?api=1&destination=${coords.lat}%2C${coords.lng}&travelmode=driving`;
}
