// Web Audio API Sound Alert Manager for GoBajaaj Admin Web

class SoundAlertManager {
  private audioCtx: AudioContext | null = null;
  private isSoundEnabled: boolean = true;
  private hasUserInteracted: boolean = false;

  constructor() {
    const saved = localStorage.getItem('gobajaaj_sound_enabled');
    if (saved !== null) {
      this.isSoundEnabled = saved === 'true';
    }

    // Attach interaction listener to unlock AudioContext on first user gesture
    if (typeof window !== 'undefined') {
      const unlockAudio = () => {
        this.hasUserInteracted = true;
        if (this.audioCtx && this.audioCtx.state === 'suspended') {
          this.audioCtx.resume();
        }
        window.removeEventListener('click', unlockAudio);
        window.removeEventListener('keydown', unlockAudio);
        window.removeEventListener('touchstart', unlockAudio);
      };
      window.addEventListener('click', unlockAudio, { once: true });
      window.addEventListener('keydown', unlockAudio, { once: true });
      window.addEventListener('touchstart', unlockAudio, { once: true });
    }
  }

  private getAudioContext(): AudioContext | null {
    try {
      if (!this.audioCtx) {
        const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
        if (AudioCtxClass) {
          this.audioCtx = new AudioCtxClass();
        }
      }
      if (this.audioCtx && this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      return this.audioCtx;
    } catch (err) {
      console.warn('AudioContext creation error:', err);
      return null;
    }
  }

  public isEnabled(): boolean {
    return this.isSoundEnabled;
  }

  public setEnabled(enabled: boolean) {
    this.isSoundEnabled = enabled;
    try {
      localStorage.setItem('gobajaaj_sound_enabled', String(enabled));
    } catch (e) {
      // Ignore storage errors
    }
  }

  // Play crisp, melodic cash chime / digital payment notification
  public playPaymentSound(): boolean {
    if (!this.isSoundEnabled) return false;

    try {
      const ctx = this.getAudioContext();
      if (!ctx) return false;

      const now = ctx.currentTime;

      // 1. Chime Part 1: Bright rising coin tone (C6 to G6)
      const osc1 = ctx.createOscillator();
      const gain1 = ctx.createGain();
      osc1.type = 'sine';
      osc1.frequency.setValueAtTime(1046.50, now); // C6
      osc1.frequency.exponentialRampToValueAtTime(1567.98, now + 0.12); // G6

      gain1.gain.setValueAtTime(0, now);
      gain1.gain.linearRampToValueAtTime(0.4, now + 0.02);
      gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.35);

      osc1.connect(gain1);
      gain1.connect(ctx.destination);
      osc1.start(now);
      osc1.stop(now + 0.35);

      // 2. Chime Part 2: High Bell Harmonic Ring (E6 to C7)
      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.type = 'triangle';
      osc2.frequency.setValueAtTime(1318.51, now + 0.10); // E6
      osc2.frequency.exponentialRampToValueAtTime(2093.00, now + 0.25); // C7

      gain2.gain.setValueAtTime(0, now + 0.10);
      gain2.gain.linearRampToValueAtTime(0.45, now + 0.13);
      gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.60);

      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.start(now + 0.10);
      osc2.stop(now + 0.60);

      // 3. Chime Part 3: Cash Register Ka-Ching Shimmer Finish
      const osc3 = ctx.createOscillator();
      const gain3 = ctx.createGain();
      osc3.type = 'sine';
      osc3.frequency.setValueAtTime(2637.02, now + 0.22); // E7
      osc3.frequency.setValueAtTime(2093.00, now + 0.35); // C7

      gain3.gain.setValueAtTime(0, now + 0.22);
      gain3.gain.linearRampToValueAtTime(0.35, now + 0.25);
      gain3.gain.exponentialRampToValueAtTime(0.0001, now + 0.80);

      osc3.connect(gain3);
      gain3.connect(ctx.destination);
      osc3.start(now + 0.22);
      osc3.stop(now + 0.80);

      return true;
    } catch (err) {
      console.warn('Payment sound error:', err);
      return false;
    }
  }

  // Browser Speech Synthesis (Fariin Cod ah oo toos ah)
  public speakVoice(text: string) {
    if (!this.isSoundEnabled) return;
    try {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 1.0;
        utterance.pitch = 1.05;
        utterance.volume = 1.0;
        window.speechSynthesis.speak(utterance);
      }
    } catch (e) {
      console.warn('Voice speak error:', e);
    }
  }

  // Combined Chime + Somali Voice Speech Alert when driver submits money ($0.25)
  public playPaymentVoiceAlert(detailText?: string) {
    this.playPaymentSound();
    const spoken = detailText || "Lacag cusub ayaa soo dhacday! Darawal ayaa soo dirsaday Driver Pass. Fadlan xaqiiji.";
    setTimeout(() => {
      this.speakVoice(spoken);
    }, 450);
  }

  // Play test sound and return result
  public testSound(): boolean {
    const ctx = this.getAudioContext();
    if (ctx && ctx.state === 'suspended') {
      ctx.resume();
    }
    this.playPaymentVoiceAlert("Tijaabada codka: GoBajaaj Admin alert waa diyaar.");
    return true;
  }
}

export const soundAlert = new SoundAlertManager();
