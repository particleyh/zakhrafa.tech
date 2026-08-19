/**
 * Zakhrafa UI Helper v8
 */
(function() {
    const Z = window.Zakhrafa;
    if (!Z) return;

    let currentTab = 'decorate';
    let currentFilter = 'all';

    const platformLabels = {
        pubg: 'ببجي',
        freefire: 'فري فاير',
        tiktok: 'تيك توك',
        instagram: 'انستقرام',
        facebook: 'فيسبوك'
    };

    function make(tag, className, text) {
        const el = document.createElement(tag);
        if (className) el.className = className;
        if (text !== undefined) el.textContent = text;
        return el;
    }

    const UI = {
        init: function(options) {
            this.config = Object.assign({
                inputId: 'inp',
                resultsId: 'results',
                statusId: 'sc',
                filter: 'all',
                platform: 'none',
                emptyIcon: '✦',
                emptyText: 'اكتب اسمك وبتظهر النتائج هنا'
            }, options || {});

            this.input = document.getElementById(this.config.inputId);
            this.container = document.getElementById(this.config.resultsId);
            this.status = document.getElementById(this.config.statusId);
            if (!this.input || !this.container) return;

            currentFilter = 'all';
            if (this.config.filter !== 'all') currentFilter = this.config.filter;
            if (this.config.platform !== 'none') currentFilter = this.config.platform;

            const path = window.location.pathname;
            document.querySelectorAll('.top-nav a').forEach(a => {
                const href = a.getAttribute('href');
                const active = href === path || (path === '/' && href === '/') || (path.endsWith('/') && href === path);
                a.classList.toggle('active', active);
                if (active) a.setAttribute('aria-current', 'page');
            });

            if (this.status) this.status.textContent = Z.countStyles() + '+ نمط';
            document.querySelectorAll('.nav-item').forEach(btn => {
                btn.addEventListener('click', () => this.switchTab(btn.dataset.tab, btn));
            });

            const startTab = this.config.tab || 'decorate';
            this.switchTab(startTab, document.querySelector(".nav-item[data-tab='" + startTab + "']"));
        },

        switchTab: function(tab, btn) {
            currentTab = tab;
            document.querySelectorAll('.nav-item').forEach(x => x.classList.remove('active'));
            if (btn) btn.classList.add('active');
            this.container.innerHTML = '';
            if (tab === 'decorate') this.setupDecorate();
            else if (tab === 'symbols') this.renderSymbols();
            else if (tab === 'fav') this.renderFavorites();
        },

        setupDecorate: function() {
            const filterBar = make('div', 'filter-row');
            const cats = [
                {id: 'all', label: 'الكل'},
                {id: 'arabic', label: 'عربي'},
                {id: 'english', label: 'انجليزي'},
                {id: 'complex', label: 'نادر'}
            ];
            Object.keys(platformLabels).forEach(p => {
                if (this.config.filter === p || this.config.platform === p) cats.push({id: p, label: platformLabels[p]});
            });

            cats.forEach(cat => {
                const btn = make('button', 'btn-filter' + (currentFilter === cat.id ? ' active' : ''), cat.label);
                btn.type = 'button';
                btn.dataset.f = cat.id;
                btn.addEventListener('click', () => {
                    currentFilter = cat.id;
                    if (currentFilter === 'english' && (this.input.value === 'زخرفة' || !/[a-zA-Z]/.test(this.input.value))) this.input.value = 'Hello';
                    else if (currentFilter === 'arabic' && (this.input.value === 'Hello' || /[a-zA-Z]/.test(this.input.value))) this.input.value = 'زخرفة';
                    filterBar.querySelectorAll('.btn-filter').forEach(x => x.classList.remove('active'));
                    btn.classList.add('active');
                    this.generate();
                });
                filterBar.appendChild(btn);
            });
            this.container.appendChild(filterBar);

            const list = make('div', 'results');
            list.id = 'decorate-list';
            this.container.appendChild(list);

            this.generate = () => {
                const text = this.input.value.trim();
                list.innerHTML = '';
                if (!text) {
                    list.appendChild(this.emptyState(this.config.emptyIcon, this.config.emptyText));
                    return;
                }

                const results = Z.generateAll(text, currentFilter, this.config.platform);
                if (!results.length) {
                    list.appendChild(this.emptyState('∅', 'مافيه نتايج'));
                    return;
                }

                results.forEach(res => list.appendChild(this.resultCard(res)));
            };

            this.input.oninput = () => {
                if (currentTab !== 'decorate') return;
                clearTimeout(this._t);
                this._t = setTimeout(this.generate, 90);
            };
            this.generate();
        },

        resultCard: function(res) {
            const card = make('article', 'r-card');
            const head = make('div', 'r-head');
            const label = make('span', 'r-label', res.style);
            const fav = make('button', 'fav-icon' + (this.isFavorite(res.text) ? ' active' : ''), '♥');
            fav.type = 'button';
            fav.setAttribute('aria-label', 'إضافة للمفضلة');
            fav.addEventListener('click', () => this.toggleFav(res.text, fav));
            head.append(label, fav);

            const isArabic = /[\u0600-\u06FF]/.test(res.text);
            const text = make('div', 'r-text' + (isArabic ? ' ar' : ''), res.text);
            const copy = make('button', 'btn-copy', 'نسخ');
            copy.type = 'button';
            copy.addEventListener('click', () => this.copy(res.text, copy));

            card.append(head, text, copy);
            return card;
        },

        emptyState: function(icon, message) {
            const empty = make('div', 'empty');
            empty.append(make('div', 'icon', icon), make('p', '', message));
            return empty;
        },

        renderSymbols: function() {
            const cats = Object.keys(Z.symbolLab || {});
            if (!cats.length) {
                this.container.appendChild(this.emptyState('★', 'مافيه رموز حالياً'));
                return;
            }

            const tabs = make('div', 'sym-tabs');
            const grid = make('div', 'sym-grid');
            grid.id = 'sym-grid';
            cats.forEach((cat, i) => {
                const btn = make('button', 'sym-tab' + (i === 0 ? ' active' : ''), cat);
                btn.type = 'button';
                btn.addEventListener('click', () => this.switchSymCat(cat, btn));
                tabs.appendChild(btn);
            });
            this.container.append(tabs, grid);
            this.switchSymCat(cats[0]);
        },

        switchSymCat: function(cat, btn) {
            if (btn) {
                document.querySelectorAll('.sym-tab').forEach(x => x.classList.remove('active'));
                btn.classList.add('active');
            }
            const grid = document.getElementById('sym-grid');
            if (!grid) return;
            grid.innerHTML = '';
            (Z.symbolLab[cat] || []).forEach(symbol => {
                const item = make('button', 'sym-item', symbol.replace(/\n/g, ''));
                item.type = 'button';
                item.setAttribute('aria-label', 'إضافة الرمز ' + item.textContent);
                item.addEventListener('click', () => {
                    this.input.value += item.textContent;
                    item.classList.add('picked');
                    setTimeout(() => item.classList.remove('picked'), 180);
                    this.input.focus();
                    if (this.generate) this.generate();
                });
                grid.appendChild(item);
            });
        },

        renderFavorites: function() {
            const favs = JSON.parse(localStorage.getItem('z_favs') || '[]');
            if (!favs.length) {
                this.container.appendChild(this.emptyState('♡', 'ما عندك مفضلات حالياً'));
                return;
            }
            const list = make('div', 'results');
            favs.forEach(text => list.appendChild(this.resultCard({text: text, style: 'مفضلة', category: 'fav'})));
            this.container.appendChild(list);
        },

        isFavorite: function(text) {
            return JSON.parse(localStorage.getItem('z_favs') || '[]').includes(text);
        },

        toggleFav: function(text, el) {
            let favs = JSON.parse(localStorage.getItem('z_favs') || '[]');
            if (favs.includes(text)) {
                favs = favs.filter(x => x !== text);
                el.classList.remove('active');
            } else {
                favs.push(text);
                el.classList.add('active');
            }
            localStorage.setItem('z_favs', JSON.stringify(favs));
        },

        copy: function(text, button) {
            const done = () => {
                const old = button.textContent;
                button.textContent = 'تم النسخ';
                button.classList.add('copied');
                setTimeout(() => {
                    button.textContent = old;
                    button.classList.remove('copied');
                }, 1300);
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(done).catch(() => this.fallbackCopy(text, done));
            } else {
                this.fallbackCopy(text, done);
            }
        },

        fallbackCopy: function(text, done) {
            const ta = document.createElement('textarea');
            ta.value = text;
            ta.setAttribute('readonly', '');
            ta.style.position = 'fixed';
            ta.style.top = '-999px';
            document.body.appendChild(ta);
            ta.select();
            try { document.execCommand('copy'); } catch (e) {}
            document.body.removeChild(ta);
            done();
        }
    };

    window.ZakhrafaUI = UI;
})();
