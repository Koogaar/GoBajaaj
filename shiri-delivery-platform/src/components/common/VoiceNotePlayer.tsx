import React, { useState, useRef, useEffect } from 'react';
import { VoiceNote } from '../../types';
import { soundEffects } from '../../utils/soundEffects';
import { Play, Pause, Volume2, Mic } from 'lucide-react';

interface VoiceNotePlayerProps {
  voiceNote: VoiceNote;
  compact?: boolean;
}

export const VoiceNotePlayer: React.FC<VoiceNotePlayerProps> = ({
  voiceNote,
  compact = false,
}) => {
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
      }
    };
  }, []);

  const handleTogglePlay = () => {
    if (isPlaying) {
      if (audioRef.current) {
        audioRef.current.pause();
      }
      setIsPlaying(false);
    } else {
      setIsPlaying(true);
      if (voiceNote.audioUrl && !voiceNote.isSimulated) {
        if (!audioRef.current) {
          audioRef.current = new Audio(voiceNote.audioUrl);
          audioRef.current.onended = () => setIsPlaying(false);
        }
        audioRef.current.play().catch(() => {
          soundEffects.playSimulatedVoice();
          setTimeout(() => setIsPlaying(false), (voiceNote.durationSeconds || 5) * 1000);
        });
      } else {
        soundEffects.playSimulatedVoice();
        setTimeout(() => {
          setIsPlaying(false);
        }, (voiceNote.durationSeconds || 5) * 1000);
      }
    }
  };

  if (compact) {
    return (
      <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 px-2.5 py-1.5 rounded-xl">
        <button
          type="button"
          onClick={handleTogglePlay}
          className={`w-7 h-7 rounded-lg flex items-center justify-center text-white transition-all shadow-2xs ${
            isPlaying ? 'bg-amber-600 animate-pulse' : 'bg-emerald-600 hover:bg-emerald-700'
          }`}
          title="Dhageyso codka macmiilka"
        >
          {isPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
        </button>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1 text-[11px] font-bold text-emerald-950 truncate">
            <Volume2 className="w-3 h-3 text-emerald-600 shrink-0" />
            <span>Codka Goobta (0:0{voiceNote.durationSeconds})</span>
          </div>
          {voiceNote.transcriptSomali && (
            <p className="text-[10px] text-emerald-800 truncate">
              {voiceNote.transcriptSomali}
            </p>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="bg-emerald-50/90 border border-emerald-200 rounded-2xl p-3 space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-xs font-bold text-emerald-950 flex items-center gap-1.5">
          <Mic className="w-3.5 h-3.5 text-emerald-600" />
          <span>Astaamaha Codka ah ee Macmiilka (Voice Landmark)</span>
        </span>
        <span className="text-[10px] font-mono font-bold bg-emerald-200/80 text-emerald-900 px-2 py-0.5 rounded-full">
          0:0{voiceNote.durationSeconds}
        </span>
      </div>

      <div className="flex items-center gap-2.5 bg-white p-2.5 rounded-xl border border-emerald-100">
        <button
          type="button"
          onClick={handleTogglePlay}
          className={`px-3 py-1.5 rounded-xl text-xs font-bold text-white transition-all flex items-center gap-1.5 shadow-2xs ${
            isPlaying ? 'bg-amber-600 animate-pulse' : 'bg-emerald-600 hover:bg-emerald-700'
          }`}
        >
          {isPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
          <span>{isPlaying ? 'Jooji Codka' : 'Dhageyso'}</span>
        </button>

        {/* Animated wave bars */}
        <div className="flex items-center gap-1 flex-1 px-1 h-6 justify-center bg-slate-50 rounded-lg">
          {[35, 70, 50, 95, 60, 85, 40, 75, 60, 90, 45, 65].map((h, i) => (
            <div
              key={i}
              className={`w-1 rounded-full transition-all duration-300 ${
                isPlaying ? 'bg-emerald-500 animate-pulse' : 'bg-slate-300'
              }`}
              style={{ height: `${isPlaying ? (h * 0.22) : 6}px` }}
            />
          ))}
        </div>
      </div>

      {voiceNote.transcriptSomali && (
        <p className="text-[11px] text-emerald-900 bg-white/70 p-2 rounded-xl border border-emerald-100 italic leading-relaxed">
          &ldquo;{voiceNote.transcriptSomali}&rdquo;
        </p>
      )}
    </div>
  );
};
