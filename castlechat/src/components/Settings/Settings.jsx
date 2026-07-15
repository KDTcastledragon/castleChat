import './Settings.css';

import { useState } from 'react';

// demo용 설정. 실제 동작 대신 localStorage에만 저장해서 구색을 갖춘다.
const SETTINGS_STORAGE_KEY = 'castlechat:settings';

function loadSettings() {
    try {
        return { ...defaultSettings, ...JSON.parse(localStorage.getItem(SETTINGS_STORAGE_KEY)) };
    } catch {
        return { ...defaultSettings };
    }
}

const defaultSettings = {
    messageNotification: true,
    notificationSound: true,
    enterToSend: true,
    language: 'ko'
};

function Settings() {
    const [settings, setSettings] = useState(loadSettings);

    function updateSetting(key, value) {
        setSettings(prev => {
            const next = { ...prev, [key]: value };
            localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(next));
            return next;
        });
    }

    return (
        <div className='SettingsContainer'>
            <h2 className='settingsTitle'>설정</h2>

            <div className='settingsSection'>
                <h3>알림</h3>

                <label className='settingsRow'>
                    <span>메시지 알림</span>
                    <input
                        type='checkbox'
                        checked={settings.messageNotification}
                        onChange={(e) => updateSetting('messageNotification', e.target.checked)}
                    />
                </label>

                <label className='settingsRow'>
                    <span>알림음</span>
                    <input
                        type='checkbox'
                        checked={settings.notificationSound}
                        onChange={(e) => updateSetting('notificationSound', e.target.checked)}
                    />
                </label>
            </div>

            <div className='settingsSection'>
                <h3>채팅</h3>

                <label className='settingsRow'>
                    <span>Enter로 메시지 전송 (Shift+Enter 줄바꿈)</span>
                    <input
                        type='checkbox'
                        checked={settings.enterToSend}
                        onChange={(e) => updateSetting('enterToSend', e.target.checked)}
                    />
                </label>
            </div>

            <div className='settingsSection'>
                <h3>일반</h3>

                <label className='settingsRow'>
                    <span>언어</span>
                    <select
                        value={settings.language}
                        onChange={(e) => updateSetting('language', e.target.value)}
                    >
                        <option value='ko'>한국어</option>
                        <option value='en'>English</option>
                    </select>
                </label>
            </div>

            <p className='settingsDemoNotice'>* 데모 설정입니다. 브라우저에만 저장되며 일부 항목은 실제 동작과 연결되어 있지 않습니다.</p>
        </div>
    );
}

export default Settings;
