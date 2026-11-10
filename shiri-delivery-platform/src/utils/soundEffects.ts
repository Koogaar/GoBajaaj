/**
 * Web Audio API Sound Synthesizer
 * Generates self-contained, crystal-clear audio notifications
 * without external MP3 files or CORS network dependencies.
 */

class SoundEffectsService {
  private audioCtx: AudioContext | null = null;
  private isMuted: boolean = false;

  constructor() {
    // AudioContext will be lazily initialized on first user interaction
    const savedMute = localStorage.getItem('shiri_sound_muted');
    if (savedMute) {
      this.isMuted = savedMute === 'true';
    }
  }

  private getAudioContext(): AudioContext | null {
    if (this.isMuted) return null;
    try {
      if (!this.audioCtx) {
        const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
        this.audioCtx = new AudioContextClass();
      }
      if (this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      return this.audioCtx;
    } catch {
      return null;
    }
  }

  getIsMuted(): boolean {
    return this.isMuted;
  }

  setMuted(muted: boolean) {
    this.isMuted = muted;
    try {
      localStorage.setItem('shiri_sound_muted', String(muted));
    } catch {}
  }

  toggleMute(): boolean {
    this.setMuted(!this.isMuted);
    return this.isMuted;
  }

  /**
   * Loud Merchant Order Alert Tone
   * Distinctive 3-burst restaurant kitchen chime (880Hz -> 1174Hz)
   */
  playOrderAlert() {
    const ctx = this.getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    const playChime = (offset: number, highPitch: boolean) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(highPitch ? 1174.66 : 880.0, now + offset);

      gain.gain.setValueAtTime(0, now + offset);
      gain.gain.linearRampToValueAtTime(0.4, now + offset + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + offset + 0.28);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now + offset);
      osc.stop(now + offset + 0.3);
    };

    // Burst 1
    playChime(0, false);
    playChime(0.12, true);

    // Burst 2
    playChime(0.35, false);
    playChime(0.47, true);

    // Burst 3
    playChime(0.7, false);
    playChime(0.82, true);
  }

  /**
   * Loud Courier Dispatch Broadcast
   * Rapid urgent motorcycle / radar ping (587Hz -> 784Hz -> 1046Hz)
   */
  playDispatchAlert() {
    const ctx = this.getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    const notes = [587.33, 783.99, 1046.5];

    notes.forEach((freq, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(freq, now + idx * 0.14);

      gain.gain.setValueAtTime(0, now + idx * 0.14);
      gain.gain.linearRampToValueAtTime(0.3, now + idx * 0.14 + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.14 + 0.18);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now + idx * 0.14);
      osc.stop(now + idx * 0.14 + 0.2);
    });
  }

  /**
   * Order Placement / Payment Success Chime
   * Warm, pleasant major chord (C5 - E5 - G5 - C6)
   */
  playSuccessChime() {
    const ctx = this.getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    const freqs = [523.25, 659.25, 783.99, 1046.5];

    freqs.forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now + i * 0.08);

      gain.gain.setValueAtTime(0, now + i * 0.08);
      gain.gain.linearRampToValueAtTime(0.25, now + i * 0.08 + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + i * 0.08 + 0.4);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now + i * 0.08);
      osc.stop(now + i * 0.08 + 0.45);
    });
  }

  /**
   * Mic Recording Beep (Start / Stop)
   */
  playRecordBeep(isStart: boolean) {
    const ctx = this.getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(isStart ? 880 : 440, now);

    gain.gain.setValueAtTime(0.15, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start(now);
    osc.stop(now + 0.13);
  }

  /**
   * Voice Playback Synthesizer Fallback
   * Synthesizes audio pulses if user plays simulated voice clips
   */
  playSimulatedVoice() {
    const ctx = this.getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    // Play speech-like formant tones
    const frequencies = [220, 330, 260, 390, 440, 350, 300, 250];
    frequencies.forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, now + i * 0.18);

      gain.gain.setValueAtTime(0, now + i * 0.18);
      gain.gain.linearRampToValueAtTime(0.18, now + i * 0.18 + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.001, now + i * 0.18 + 0.16);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now + i * 0.18);
      osc.stop(now + i * 0.18 + 0.17);
    });
  }
}

export const soundEffects = new SoundEffectsService();
