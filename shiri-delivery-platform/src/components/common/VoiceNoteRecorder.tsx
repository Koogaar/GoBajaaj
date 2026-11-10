import React, { useState, useRef, useEffect } from 'react';
import { VoiceNote } from '../../types';
import { soundEffects } from '../../utils/soundEffects';
import { Mic, Square, Play, Pause, Trash2, CheckCircle2, Sparkles, Volume2 } from 'lucide-react';

interface VoiceNoteRecorderProps {
  onVoiceNoteRecorded: (voiceNote: VoiceNote | null) => void;
  existingVoiceNote?: VoiceNote;
}

export const VoiceNoteRecorder: React.FC<VoiceNoteRecorderProps> = ({
  onVoiceNoteRecorded,
  existingVoiceNote,
}) => {
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [recordingSeconds, setRecordingSeconds] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [activeVoiceNote, setActiveVoiceNote] = useState<VoiceNote | null>(existingVoiceNote || null);
  const [micError, setMicError] = useState<string | null>(null);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const timerRef = useRef<number | null>(null);
  const audioPlayerRef = useRef<HTMLAudioElement | null>(null);

  // Quick pre-recorded Somali landmark audio presets for quick 1-tap testing
  const presets = [
    {
      title: 'Hodan (Digfeer)',
      text: 'Agta Masaajidka Digfeer, guri dabaq ah oo cagaar ah, albaabka birta ah ku dhufo.',
      duration: 8,
    },
    {
      title: 'Waberi (KM4)',
      text: 'Maka Al-Mukarama dhinaca ka soo horjeeda Hotel Maka, dabaqa 2-aad.',
      duration: 6,
    },
    {
      title: 'Wadajir (Medina)',
      text: 'Dhabarka Isbitaalka Medina, ka soo leexo laamiga, guri cad oo leh barxad weyn.',
      duration: 10,
    },
  ];

  // Stop recording cleanup
  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (audioPlayerRef.current) {
        audioPlayerRef.current.pause();
      }
    };
  }, []);

  const startLiveRecording = async () => {
    setMicError(null);
    audioChunksRef.current = [];

    try {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const mediaRecorder = new MediaRecorder(stream);
        mediaRecorderRef.current = mediaRecorder;

        mediaRecorder.ondataavailable = (event) => {
          if (event.data.size > 0) {
            audioChunksRef.current.push(event.data);
          }
        };

        mediaRecorder.onstop = () => {
          const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
          const audioUrl = URL.createObjectURL(audioBlob);
          const duration = recordingSeconds || 5;

          const newNote: VoiceNote = {
            id: `vn-${Date.now()}`,
            audioUrl,
            durationSeconds: duration,
            recordedAt: new Date().toISOString(),
            transcriptSomali: 'Codka macmiilka: Tilmaamaha goobta iyo calaamadaha guriga.',
            isSimulated: false,
          };

          setActiveVoiceNote(newNote);
          onVoiceNoteRecorded(newNote);

          // Stop tracks to release mic
          stream.getTracks().forEach((track) => track.stop());
        };

        soundEffects.playRecordBeep(true);
        mediaRecorder.start();
        setIsRecording(true);
        setRecordingSeconds(0);

        timerRef.current = window.setInterval(() => {
          setRecordingSeconds((prev) => {
            if (prev >= 25) {
              stopRecording();
              return 25;
            }
            return prev + 1;
          });
        }, 1000);
      } else {
        throw new Error('Microphone not supported on this device');
      }
    } catch {
      // Browser mic permission blocked or not available in iframe sandbox: use high-fidelity simulated Somali landmark preset
      setMicError('Mic direct permission is unavailable in iframe. Loading Somali landmark voice preset:');
      selectPreset(presets[0]);
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      soundEffects.playRecordBeep(false);
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    }
  };

  const selectPreset = (preset: typeof presets[0]) => {
    soundEffects.playSuccessChime();
    const newNote: VoiceNote = {
      id: `vn-preset-${Date.now()}`,
      durationSeconds: preset.duration,
      recordedAt: new Date().toISOString(),
      transcriptSomali: preset.text,
      isSimulated: true,
    };
    setActiveVoiceNote(newNote);
    onVoiceNoteRecorded(newNote);
  };

  const handlePlayVoice = () => {
    if (!activeVoiceNote) return;

    if (isPlaying) {
      if (audioPlayerRef.current) {
        audioPlayerRef.current.pause();
      }
      setIsPlaying(false);
    } else {
      setIsPlaying(true);
      if (activeVoiceNote.audioUrl && !activeVoiceNote.isSimulated) {
        if (!audioPlayerRef.current) {
          audioPlayerRef.current = new Audio(activeVoiceNote.audioUrl);
          audioPlayerRef.current.onended = () => setIsPlaying(false);
        }
        audioPlayerRef.current.play().catch(() => {
          soundEffects.playSimulatedVoice();
          setTimeout(() => setIsPlaying(false), (activeVoiceNote.durationSeconds || 4) * 1000);
        });
      } else {
        // Synthesizer voice playback
        soundEffects.playSimulatedVoice();
        setTimeout(() => {
          setIsPlaying(false);
        }, (activeVoiceNote.durationSeconds || 4) * 1000);
      }
    }
  };

  const handleDeleteVoice = () => {
    if (audioPlayerRef.current) {
      audioPlayerRef.current.pause();
    }
    setIsPlaying(false);
    setActiveVoiceNote(null);
    onVoiceNoteRecorded(null);
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50/60 p-3.5 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <div className="w-6 h-6 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-xs">
            🎙️
          </div>
          <div>
            <span className="text-xs font-bold text-slate-900 block leading-tight">
              Astaamaha Codka ah (Voice Landmark Note)
            </span>
            <span className="text-[10px] text-slate-500 block">
              Ku duub cod 15 ilbiriqsi ah oo darawalka ku tusaya albaabka
            </span>
          </div>
        </div>
        {activeVoiceNote && (
          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 flex items-center gap-1">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            <span>Cod Diyaar ah</span>
          </span>
        )}
      </div>

      {/* If a voice note is recorded or selected */}
      {activeVoiceNote ? (
        <div className="bg-white rounded-xl p-3 border border-emerald-200/80 shadow-2xs space-y-2">
          <div className="flex items-center justify-between gap-2">
            <button
              type="button"
              onClick={handlePlayVoice}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all shadow-xs ${
                isPlaying
                  ? 'bg-amber-600 text-white animate-pulse'
                  : 'bg-emerald-600 hover:bg-emerald-700 text-white'
              }`}
            >
              {isPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
              <span>{isPlaying ? 'Jooji Codka' : 'Dhageyso Codka'}</span>
            </button>

            {/* Audio Waveform Visualizer */}
            <div className="flex items-center gap-1 flex-1 px-2 h-7 justify-center bg-slate-50 rounded-lg">
              {[40, 75, 55, 90, 60, 100, 45, 80, 65, 95, 50, 70].map((h, i) => (
                <div
                  key={i}
                  className={`w-1 rounded-full transition-all duration-300 ${
                    isPlaying ? 'bg-emerald-500 animate-pulse' : 'bg-slate-300'
                  }`}
                  style={{ height: `${isPlaying ? (h * 0.24) : 8}px` }}
                />
              ))}
            </div>

            <span className="font-mono text-xs font-bold text-slate-600 shrink-0">
              0:0{activeVoiceNote.durationSeconds}
            </span>

            <button
              type="button"
              onClick={handleDeleteVoice}
              className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
              title="Tir codka"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>

          {activeVoiceNote.transcriptSomali && (
            <p className="text-[11px] text-slate-600 bg-slate-50 p-2 rounded-lg border border-slate-100 italic leading-relaxed">
              &ldquo;{activeVoiceNote.transcriptSomali}&rdquo;
            </p>
          )}
        </div>
      ) : (
        /* If no voice note yet: Record or choose preset */
        <div className="space-y-2.5">
          {isRecording ? (
            <div className="flex items-center justify-between bg-red-50 border border-red-200 rounded-xl p-3">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-red-600 animate-ping" />
                <span className="text-xs font-bold text-red-900">
                  Duubaya codkaaga... 0:{recordingSeconds < 10 ? `0${recordingSeconds}` : recordingSeconds}
                </span>
              </div>
              <button
                type="button"
                onClick={stopRecording}
                className="flex items-center gap-1 px-3 py-1.5 bg-red-600 text-white rounded-lg text-xs font-bold hover:bg-red-700 transition-colors"
              >
                <Square className="w-3.5 h-3.5" />
                <span>Dhammee</span>
              </button>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row gap-2">
              <button
                type="button"
                onClick={startLiveRecording}
                className="flex-1 py-2 px-3 rounded-xl bg-red-600 hover:bg-red-700 active:scale-95 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-xs transition-all"
              >
                <Mic className="w-4 h-4" />
                <span>Guji oo Duub Cod (Mic)</span>
              </button>
            </div>
          )}

          {micError && (
            <p className="text-[10px] text-amber-700 font-medium">
              {micError}
            </p>
          )}

          {/* Quick 1-tap Somali landmark presets */}
          <div>
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">
              Ama Dooro Cod Diyaar ah oo Tusaale ah:
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-1.5">
              {presets.map((preset, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => selectPreset(preset)}
                  className="px-2 py-1.5 rounded-lg border border-slate-200 bg-white hover:border-red-400 hover:bg-red-50/40 text-left text-[11px] font-medium text-slate-700 transition-colors truncate"
                  title={preset.text}
                >
                  <span className="font-bold text-slate-900 block truncate">{preset.title}</span>
                  <span className="text-[10px] text-slate-500 truncate block">{preset.text}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
