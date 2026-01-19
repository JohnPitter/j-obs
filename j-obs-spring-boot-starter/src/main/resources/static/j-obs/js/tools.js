function toolsPage() {
    return {
        activeTab: 'service-map',
        showSloModal: false,
        anomalyTimeRange: '24h',

        // Service Map
        serviceMap: {
            loading: false,
            services: [],
            connections: []
        },

        // Profiling
        profiling: {
            cpu: {
                duration: '60',
                interval: '10',
                running: false,
                result: null
            },
            memory: {
                heapUsed: '0 MB',
                heapPercent: 0,
                gcPauses: '0ms'
            },
            threads: []
        },

        // SLOs
        slos: [],

        // Anomalies
        anomalies: [],

        // SQL Problems
        sqlProblems: [],

        init() {
            this.loadServiceMap();
            this.loadMemoryStats();
            this.loadSlos();
            this.loadAnomalies();

            // Refresh memory stats periodically
            setInterval(() => this.loadMemoryStats(), 5000);
        },

        // Service Map Methods
        async loadServiceMap() {
            this.serviceMap.loading = true;
            try {
                const response = await fetch('{{BASE_PATH}}/api/tools/service-map');
                if (response.ok) {
                    const data = await response.json();
                    this.serviceMap.services = data.services || [];
                    this.serviceMap.connections = data.connections || [];
                }
            } catch (e) {
                console.error('Failed to load service map:', e);
            }
            this.serviceMap.loading = false;
        },

        getServiceIcon(type) {
            const icons = {
                'application': '🖥️',
                'database': '🗄️',
                'payment': '💳',
                'notification': '📧',
                'service': '⚙️',
                'http': '🌐',
                'messaging': '📨',
                'cache': '⚡'
            };
            return icons[type] || '📦';
        },

        getServiceColor(type) {
            const colors = {
                'application': 'from-indigo-500/20 to-purple-500/20 border-indigo-500/30',
                'database': 'from-green-500/20 to-emerald-500/20 border-green-500/30',
                'payment': 'from-amber-500/20 to-orange-500/20 border-amber-500/30',
                'notification': 'from-blue-500/20 to-cyan-500/20 border-blue-500/30',
                'service': 'from-slate-500/20 to-gray-500/20 border-slate-500/30',
                'http': 'from-pink-500/20 to-rose-500/20 border-pink-500/30',
                'messaging': 'from-yellow-500/20 to-amber-500/20 border-yellow-500/30',
                'cache': 'from-red-500/20 to-orange-500/20 border-red-500/30'
            };
            return colors[type] || 'from-slate-500/20 to-gray-500/20 border-slate-500/30';
        },

        refreshServiceMap() {
            this.loadServiceMap();
        },

        // Profiling Methods
        async startCpuProfile() {
            this.profiling.cpu.running = true;
            this.profiling.cpu.result = null;
            try {
                const response = await fetch('{{BASE_PATH}}/api/tools/profiling/cpu/start', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        duration: parseInt(this.profiling.cpu.duration),
                        interval: parseInt(this.profiling.cpu.interval)
                    })
                });
                if (response.ok) {
                    const data = await response.json();
                    // Poll for results
                    this.pollCpuProfileResult(data.sessionId);
                }
            } catch (e) {
                console.error('Failed to start CPU profile:', e);
                this.profiling.cpu.running = false;
            }
        },

        async pollCpuProfileResult(sessionId) {
            const poll = async () => {
                try {
                    const response = await fetch(`{{BASE_PATH}}/api/tools/profiling/cpu/${sessionId}`);
                    if (response.ok) {
                        const data = await response.json();
                        if (data.status === 'completed') {
                            this.profiling.cpu.result = data;
                            this.profiling.cpu.running = false;
                        } else if (data.status === 'running') {
                            setTimeout(poll, 2000);
                        }
                    }
                } catch (e) {
                    console.error('Failed to poll CPU profile:', e);
                    this.profiling.cpu.running = false;
                }
            };
            poll();
        },

        async loadMemoryStats() {
            try {
                const response = await fetch('{{BASE_PATH}}/api/tools/profiling/memory');
                if (response.ok) {
                    const data = await response.json();
                    this.profiling.memory.heapUsed = data.heapUsed || '0 MB';
                    this.profiling.memory.heapPercent = data.heapPercent || 0;
                    this.profiling.memory.gcPauses = data.gcPauses || '0ms';
                }
            } catch (e) {
                // Silently fail - memory stats are optional
            }
        },

        async triggerHeapDump() {
            try {
                const response = await fetch('{{BASE_PATH}}/api/tools/profiling/heap-dump', {
                    method: 'POST'
                });
                if (response.ok) {
                    const data = await response.json();
                    alert('Heap dump saved to: ' + data.path);
                }
            } catch (e) {
                alert('Failed to trigger heap dump');
            }
        },

        async triggerGc() {
            try {
                await fetch('{{BASE_PATH}}/api/tools/profiling/gc', { method: 'POST' });
                this.loadMemoryStats();
            } catch (e) {
                console.error('Failed to trigger GC:', e);
            }
        },

        async captureThreadDump() {
            try {
                const response = await fetch('{{BASE_PATH}}/api/tools/profiling/threads');
                if (response.ok) {
                    const data = await response.json();
                    this.profiling.threads = data.threads || [];
                }
            } catch (e) {
                console.error('Failed to capture thread dump:', e);
            }
        },

        // SLO Methods
        async loadSlos() {
            try {
                const response = await fetch('{{BASE_PATH}}/api/tools/slos');
                if (response.ok) {
                    const data = await response.json();
                    this.slos = data.slos || [];
                }
            } catch (e) {
                console.error('Failed to load SLOs:', e);
            }
        },

        // Anomaly Methods
        async loadAnomalies() {
            try {
                const response = await fetch(`{{BASE_PATH}}/api/tools/anomalies?range=${this.anomalyTimeRange}`);
                if (response.ok) {
                    const data = await response.json();
                    this.anomalies = data.anomalies || [];
                }
            } catch (e) {
                console.error('Failed to load anomalies:', e);
            }
        },

        refreshAnomalies() {
            this.loadAnomalies();
        },

        // SQL Analyzer Methods
        async analyzeSql() {
            try {
                const response = await fetch('{{BASE_PATH}}/api/tools/sql/analyze', { method: 'POST' });
                if (response.ok) {
                    const data = await response.json();
                    this.sqlProblems = data.problems || [];
                }
            } catch (e) {
                console.error('Failed to analyze SQL:', e);
            }
        }
    }
}
