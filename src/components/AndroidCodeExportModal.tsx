import React, { useState } from 'react';
import { 
  ANDROID_MAIN_ACTIVITY_KT, 
  ANDROID_CAMERA2_MANAGER_KT,
  ANDROID_DUAL_VIDEO_RECORDER_KT, 
  ANDROID_LEICA_COLOR_PIPELINE_KT,
  ANDROID_THERMAL_MONITOR_KT,
  ANDROID_MANIFEST_XML,
  ANDROID_BUILD_GRADLE_KTS
} from '../services/androidCode';
import { X, Copy, Check, Download, Code2, FileCode } from 'lucide-react';

interface AndroidCodeExportModalProps {
  isOpen: boolean;
  onClose: () => void;
}

type TabType = 'main' | 'camera2' | 'dual' | 'leica' | 'thermal' | 'manifest' | 'gradle';

export const AndroidCodeExportModal: React.FC<AndroidCodeExportModalProps> = ({
  isOpen,
  onClose
}) => {
  const [activeTab, setActiveTab] = useState<TabType>('main');
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const fileMap: Record<TabType, { code: string; name: string }> = {
    main: { code: ANDROID_MAIN_ACTIVITY_KT, name: 'MainActivity.kt' },
    camera2: { code: ANDROID_CAMERA2_MANAGER_KT, name: 'Camera2Manager.kt' },
    dual: { code: ANDROID_DUAL_VIDEO_RECORDER_KT, name: 'DualVideoRecorder.kt' },
    leica: { code: ANDROID_LEICA_COLOR_PIPELINE_KT, name: 'LeicaColorPipeline.kt' },
    thermal: { code: ANDROID_THERMAL_MONITOR_KT, name: 'ThermalMonitor.kt' },
    manifest: { code: ANDROID_MANIFEST_XML, name: 'AndroidManifest.xml' },
    gradle: { code: ANDROID_BUILD_GRADLE_KTS, name: 'app/build.gradle.kts' }
  };

  const current = fileMap[activeTab];

  const handleCopy = () => {
    navigator.clipboard.writeText(current.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownload = () => {
    const blob = new Blob([current.code], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = current.name.split('/').pop() || current.name;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/85 backdrop-blur-md">
      <div className="bg-neutral-900 border border-neutral-800 text-neutral-100 rounded-2xl w-full max-w-5xl max-h-[92vh] flex flex-col shadow-2xl overflow-hidden font-sans">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 bg-neutral-950">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-red-600/20 text-red-400 border border-red-500/30">
              <Code2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold tracking-wide">Xiaomi 12S Ultra Camera2 Kotlin Architecture</h2>
              <p className="text-xs text-neutral-400">Android 15 (minSdk 35) · Jetpack Compose · HEVC HLG 10-Bit BT.2020</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Selector */}
        <div className="flex items-center justify-between px-6 py-2.5 bg-neutral-950/70 border-b border-neutral-800 overflow-x-auto gap-2">
          <div className="flex items-center gap-1.5 flex-nowrap">
            {(
              [
                ['main', 'MainActivity.kt'],
                ['camera2', 'Camera2Manager.kt'],
                ['dual', 'DualVideoRecorder.kt'],
                ['leica', 'LeicaColorPipeline.kt'],
                ['thermal', 'ThermalMonitor.kt'],
                ['manifest', 'AndroidManifest.xml'],
                ['gradle', 'build.gradle.kts']
              ] as const
            ).map(([tab, label]) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-mono font-bold whitespace-nowrap transition-all ${
                  activeTab === tab
                    ? 'bg-amber-400 text-black shadow'
                    : 'bg-neutral-800/80 text-neutral-400 hover:text-white'
                }`}
              >
                <FileCode className="w-3.5 h-3.5" />
                {label}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2 pl-4">
            <button
              onClick={handleCopy}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-medium transition-colors"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              {copied ? 'Copied' : 'Copy'}
            </button>
            <button
              onClick={handleDownload}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-600 hover:bg-red-500 text-white text-xs font-medium transition-colors"
            >
              <Download className="w-3.5 h-3.5" />
              Download
            </button>
          </div>
        </div>

        {/* Code Content */}
        <div className="flex-1 overflow-y-auto p-4 bg-black/60 font-mono text-xs text-neutral-200 select-text">
          <pre className="whitespace-pre overflow-x-auto leading-relaxed">
            <code>{current.code}</code>
          </pre>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-neutral-800 bg-neutral-950 flex items-center justify-between text-xs text-neutral-400">
          <span>Targeting Snapdragon 8+ Gen 1 · Sony IMX989 (1") & Sony IMX586</span>
          <span className="text-amber-400 font-mono">DaVinci Resolve Log BT.2020 Verified</span>
        </div>

      </div>
    </div>
  );
};
