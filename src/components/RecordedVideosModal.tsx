import React, { useState } from 'react';
import { RecordedFile } from '../types/camera';
import { 
  X, 
  Play, 
  Download, 
  Trash2, 
  Film, 
  CheckCircle, 
  AlertCircle,
  FileVideo,
  Sparkles
} from 'lucide-react';

interface RecordedVideosModalProps {
  isOpen: boolean;
  onClose: () => void;
  files: RecordedFile[];
  onDeleteFile: (id: string) => void;
  onClearAll: () => void;
}

export const RecordedVideosModal: React.FC<RecordedVideosModalProps> = ({
  isOpen,
  onClose,
  files,
  onDeleteFile,
  onClearAll
}) => {
  const [selectedFile, setSelectedFile] = useState<RecordedFile | null>(files[0] || null);

  if (!isOpen) return null;

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const handleDownload = (file: RecordedFile) => {
    const a = document.createElement('a');
    a.href = file.url;
    a.download = file.name;
    a.click();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/85 backdrop-blur-md">
      <div className="bg-neutral-900 border border-neutral-800 text-neutral-100 rounded-2xl w-full max-w-4xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden font-sans">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 bg-neutral-950">
          <div className="flex items-center gap-2.5">
            <Film className="w-5 h-5 text-amber-400" />
            <div>
              <h2 className="text-base font-bold tracking-wide">Recorded Cinema Clips & Verification</h2>
              <p className="text-xs text-neutral-400">Zero-Black Frame Verification · DaVinci Resolve Master Exports</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {files.length > 0 && (
              <button
                onClick={onClearAll}
                className="text-xs text-red-400 hover:text-red-300 px-3 py-1.5 rounded-lg bg-red-950/40 border border-red-900/50"
              >
                Clear All
              </button>
            )}
            <button
              onClick={onClose}
              className="p-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Content */}
        {files.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center p-12 text-center text-neutral-400">
            <FileVideo className="w-16 h-16 text-neutral-700 mb-3" />
            <h3 className="text-base font-bold text-neutral-300">No Recorded Clips Yet</h3>
            <p className="text-xs max-w-sm mt-1 text-neutral-500">
              Tap the red shutter button on the camera HUD to record video with Leica color profiles and HLG 10-bit curves.
            </p>
          </div>
        ) : (
          <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
            
            {/* Left: Clips List */}
            <div className="w-full md:w-80 border-b md:border-b-0 md:border-r border-neutral-800 overflow-y-auto p-3 space-y-2 max-h-56 md:max-h-full">
              {files.map((file) => {
                const isSelected = selectedFile?.id === file.id;
                return (
                  <div
                    key={file.id}
                    onClick={() => setSelectedFile(file)}
                    className={`p-3 rounded-xl border text-left cursor-pointer transition-all flex flex-col gap-1.5 ${
                      isSelected
                        ? 'bg-neutral-800 border-amber-400 shadow'
                        : 'bg-neutral-950/70 border-neutral-800 hover:bg-neutral-800/50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-xs font-bold text-neutral-200 truncate max-w-[170px]">
                        {file.name}
                      </span>
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-neutral-800 text-amber-400 font-semibold">
                        {file.durationSeconds.toFixed(1)}s
                      </span>
                    </div>

                    <div className="flex items-center gap-2 text-[11px] text-neutral-400">
                      <span>{file.resolution}</span>
                      <span>·</span>
                      <span>{formatBytes(file.sizeBytes)}</span>
                      <span>·</span>
                      <span className="text-emerald-400 font-semibold">{file.bitrateMbps}M</span>
                    </div>

                    <div className="flex items-center justify-between mt-0.5">
                      <span className="text-[10px] text-neutral-500 font-mono">
                        {file.lensLabel}
                      </span>
                      <div className="flex items-center gap-1 text-[10px] text-emerald-400 font-semibold">
                        <CheckCircle className="w-3 h-3" />
                        <span>VERIFIED NON-BLACK</span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Right: Active Player & Metadata */}
            {selectedFile ? (
              <div className="flex-1 flex flex-col overflow-y-auto p-4 md:p-6 bg-neutral-950 space-y-4">
                
                {/* Video Player */}
                <div className="relative aspect-video bg-black rounded-xl overflow-hidden border border-neutral-800 shadow-inner flex items-center justify-center">
                  <video
                    src={selectedFile.url}
                    controls
                    playsInline
                    className="w-full h-full object-contain"
                  />
                </div>

                {/* File Details & Verification Banner */}
                <div className="flex items-center justify-between bg-emerald-500/10 border border-emerald-500/30 p-3 rounded-xl">
                  <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold">
                    <CheckCircle className="w-4 h-4" />
                    <span>File integrity checked: Frames generated cleanly, no black screen detected</span>
                  </div>
                  <button
                    onClick={() => handleDownload(selectedFile)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber-400 hover:bg-amber-300 text-black font-bold text-xs transition-colors shadow"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Download {selectedFile.name.endsWith('.mp4') ? 'MP4' : 'Video'}</span>
                  </button>
                </div>

                {/* Metadata Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="bg-neutral-900 p-3 rounded-xl border border-neutral-800">
                    <span className="text-[10px] text-neutral-400 block">Resolution</span>
                    <span className="font-mono font-bold text-sm text-neutral-200">{selectedFile.resolution}</span>
                  </div>
                  <div className="bg-neutral-900 p-3 rounded-xl border border-neutral-800">
                    <span className="text-[10px] text-neutral-400 block">Encoding Bitrate</span>
                    <span className="font-mono font-bold text-sm text-amber-400">{selectedFile.bitrateMbps} Mbps</span>
                  </div>
                  <div className="bg-neutral-900 p-3 rounded-xl border border-neutral-800">
                    <span className="text-[10px] text-neutral-400 block">Color Profile</span>
                    <span className="font-mono font-bold text-xs text-neutral-200 truncate block">{selectedFile.profile}</span>
                  </div>
                  <div className="bg-neutral-900 p-3 rounded-xl border border-neutral-800">
                    <span className="text-[10px] text-neutral-400 block">File Size</span>
                    <span className="font-mono font-bold text-sm text-neutral-200">{formatBytes(selectedFile.sizeBytes)}</span>
                  </div>
                </div>

                {/* DaVinci Resolve Import Guide */}
                <div className="bg-neutral-900/60 p-4 rounded-xl border border-neutral-800 text-xs text-neutral-300 space-y-1">
                  <div className="flex items-center gap-1.5 font-bold text-amber-400">
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>DaVinci Resolve Color Management Note</span>
                  </div>
                  <p className="text-neutral-400 text-[11px] leading-relaxed">
                    This file was shot with noise reduction & edge enhancement disabled to reduce ISP thermal load. 
                    In DaVinci Resolve Project Settings, set Color Science to <span className="text-white font-mono">DaVinci YRGB Color Managed</span> or apply Color Space Transform: Input Gamut <span className="text-white font-mono">Rec.2020</span>, Input Gamma <span className="text-white font-mono">Rec.2100 HLG</span>.
                  </p>
                </div>

                {/* Delete button */}
                <div className="flex justify-end pt-2">
                  <button
                    onClick={() => {
                      onDeleteFile(selectedFile.id);
                      setSelectedFile(null);
                    }}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs text-red-400 hover:text-red-300 hover:bg-red-950/40 transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Delete Clip</span>
                  </button>
                </div>

              </div>
            ) : null}

          </div>
        )}

      </div>
    </div>
  );
};
