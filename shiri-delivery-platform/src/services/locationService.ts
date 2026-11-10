import { Coordinates } from '../types';

export interface LocationState {
  coords: Coordinates | null;
  accuracyMeters: number | null;
  status: 'idle' | 'requesting' | 'granted' | 'denied' | 'unavailable' | 'timeout';
  errorMessage: string | null;
  timestamp: number | null;
}

export class LocationService {
  /**
   * Request device's actual GPS position using standard HTML5 Geolocation API
   */
  static getCurrentLocation(
    onSuccess: (coords: Coordinates, accuracy: number) => void,
    onError: (status: 'denied' | 'unavailable' | 'timeout', message: string) => void
  ): void {
    if (!('geolocation' in navigator)) {
      onError('unavailable', 'Geolocation is not supported by your browser or device.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const coords: Coordinates = {
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        };
        const accuracy = position.coords.accuracy || 10;
        onSuccess(coords, accuracy);
      },
      (error) => {
        switch (error.code) {
          case error.PERMISSION_DENIED:
            onError(
              'denied',
              'Location permission was denied. To enable: click the lock or settings icon in your browser address bar and allow Location access. You can also adjust the pin manually or paste coordinates.'
            );
            break;
          case error.POSITION_UNAVAILABLE:
            onError(
              'unavailable',
              'GPS location unavailable. Ensure your device GPS/Location service is switched on.'
            );
            break;
          case error.TIMEOUT:
            onError(
              'timeout',
              'Location request timed out. Please check your GPS signal and try again.'
            );
            break;
          default:
            onError('unavailable', error.message || 'An unknown location error occurred.');
            break;
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 12000,
        maximumAge: 0,
      }
    );
  }

  /**
   * Watch device's position for real courier tracking
   */
  static watchLocation(
    onLocationUpdate: (coords: Coordinates, accuracy: number) => void,
    onError: (message: string) => void
  ): number | null {
    if (!('geolocation' in navigator)) {
      onError('Geolocation is not supported by this device.');
      return null;
    }

    return navigator.geolocation.watchPosition(
      (position) => {
        onLocationUpdate(
          {
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          },
          position.coords.accuracy || 10
        );
      },
      (error) => {
        onError(error.message);
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 5000,
      }
    );
  }

  static clearWatch(watchId: number): void {
    if ('geolocation' in navigator && watchId !== null) {
      navigator.geolocation.clearWatch(watchId);
    }
  }
}
