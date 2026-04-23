let items = [];
let groups = [];
let selectedGroupIndex = -1;
let selectedTradeIndex = -1;
let editingTrade = null;
let currentNbtTarget = null; // 'input' or 'output'
let draggingGroupIndex = -1;
let draggingTradeIndex = -1;
let hasUnsavedChanges = false;
let lastSavedGroupsSnapshot = '[]';
const DEBUG = false;

const NBT_PRESET_CATEGORIES = [
    {
        key: 'common',
        label: '通用高频',
        presets: [
            { label: '耐久III', nbt: { Enchantments: [{ id: 'minecraft:unbreaking', lvl: 3 }] } },
            { label: '经验修补', nbt: { Enchantments: [{ id: 'minecraft:mending', lvl: 1 }] } }
        ]
    },
    {
        key: 'armor',
        label: '装备附魔',
        presets: [
            { label: '保护IV', nbt: { Enchantments: [{ id: 'minecraft:protection', lvl: 4 }] } },
            { label: '爆炸保护IV', nbt: { Enchantments: [{ id: 'minecraft:blast_protection', lvl: 4 }] } },
            { label: '火焰保护IV', nbt: { Enchantments: [{ id: 'minecraft:fire_protection', lvl: 4 }] } },
            { label: '弹射物保护IV', nbt: { Enchantments: [{ id: 'minecraft:projectile_protection', lvl: 4 }] } },
            { label: '荆棘III', nbt: { Enchantments: [{ id: 'minecraft:thorns', lvl: 3 }] } },
            { label: '摔落缓冲IV', nbt: { Enchantments: [{ id: 'minecraft:feather_falling', lvl: 4 }] } },
            { label: '水下呼吸III', nbt: { Enchantments: [{ id: 'minecraft:respiration', lvl: 3 }] } },
            { label: '水下速掘', nbt: { Enchantments: [{ id: 'minecraft:aqua_affinity', lvl: 1 }] } },
            { label: '深海探索者III', nbt: { Enchantments: [{ id: 'minecraft:depth_strider', lvl: 3 }] } },
            { label: '冰霜行者II', nbt: { Enchantments: [{ id: 'minecraft:frost_walker', lvl: 2 }] } },
            { label: '灵魂疾行III', nbt: { Enchantments: [{ id: 'minecraft:soul_speed', lvl: 3 }] } },
            { label: '迅捷潜行III', nbt: { Enchantments: [{ id: 'minecraft:swift_sneak', lvl: 3 }] } }
        ]
    },
    {
        key: 'tool',
        label: '工具/钓鱼附魔',
        presets: [
            { label: '效率V', nbt: { Enchantments: [{ id: 'minecraft:efficiency', lvl: 5 }] } },
            { label: '时运III', nbt: { Enchantments: [{ id: 'minecraft:fortune', lvl: 3 }] } },
            { label: '精准采集', nbt: { Enchantments: [{ id: 'minecraft:silk_touch', lvl: 1 }] } },
            { label: '海之眷顾III', nbt: { Enchantments: [{ id: 'minecraft:luck_of_the_sea', lvl: 3 }] } },
            { label: '饵钓III', nbt: { Enchantments: [{ id: 'minecraft:lure', lvl: 3 }] } }
        ]
    },
    {
        key: 'melee',
        label: '近战武器附魔',
        presets: [
            { label: '锋利V', nbt: { Enchantments: [{ id: 'minecraft:sharpness', lvl: 5 }] } },
            { label: '亡灵杀手V', nbt: { Enchantments: [{ id: 'minecraft:smite', lvl: 5 }] } },
            { label: '节肢杀手V', nbt: { Enchantments: [{ id: 'minecraft:bane_of_arthropods', lvl: 5 }] } },
            { label: '抢夺III', nbt: { Enchantments: [{ id: 'minecraft:looting', lvl: 3 }] } },
            { label: '火焰附加II', nbt: { Enchantments: [{ id: 'minecraft:fire_aspect', lvl: 2 }] } },
            { label: '击退II', nbt: { Enchantments: [{ id: 'minecraft:knockback', lvl: 2 }] } },
            { label: '横扫之刃III', nbt: { Enchantments: [{ id: 'minecraft:sweeping', lvl: 3 }] } }
        ]
    },
    {
        key: 'ranged',
        label: '弓弩/三叉戟附魔',
        presets: [
            { label: '力量V', nbt: { Enchantments: [{ id: 'minecraft:power', lvl: 5 }] } },
            { label: '冲击II', nbt: { Enchantments: [{ id: 'minecraft:punch', lvl: 2 }] } },
            { label: '火矢', nbt: { Enchantments: [{ id: 'minecraft:flame', lvl: 1 }] } },
            { label: '无限', nbt: { Enchantments: [{ id: 'minecraft:infinity', lvl: 1 }] } },
            { label: '多重射击', nbt: { Enchantments: [{ id: 'minecraft:multishot', lvl: 1 }] } },
            { label: '快速装填III', nbt: { Enchantments: [{ id: 'minecraft:quick_charge', lvl: 3 }] } },
            { label: '穿透IV', nbt: { Enchantments: [{ id: 'minecraft:piercing', lvl: 4 }] } },
            { label: '忠诚III', nbt: { Enchantments: [{ id: 'minecraft:loyalty', lvl: 3 }] } },
            { label: '穿刺V', nbt: { Enchantments: [{ id: 'minecraft:impaling', lvl: 5 }] } },
            { label: '激流III', nbt: { Enchantments: [{ id: 'minecraft:riptide', lvl: 3 }] } },
            { label: '引雷', nbt: { Enchantments: [{ id: 'minecraft:channeling', lvl: 1 }] } }
        ]
    },
    {
        key: 'book',
        label: '附魔书补充',
        presets: [
            { label: '绑定诅咒', nbt: { StoredEnchantments: [{ id: 'minecraft:binding_curse', lvl: 1 }] } },
            { label: '消失诅咒', nbt: { StoredEnchantments: [{ id: 'minecraft:vanishing_curse', lvl: 1 }] } }
        ]
    }
];

function debugLog(...args) {
    if (DEBUG) {
        console.log(...args);
    }
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function getGroupsSnapshot() {
    return JSON.stringify(groups);
}

function updateSaveStatus() {
    const statusEl = document.getElementById('saveStatus');
    if (!statusEl) {
        return;
    }

    if (hasUnsavedChanges) {
        statusEl.textContent = '配置有更改（未保存）';
        statusEl.classList.add('dirty');
    } else {
        statusEl.textContent = '';
        statusEl.classList.remove('dirty');
    }
}

function refreshUnsavedChangesState() {
    hasUnsavedChanges = getGroupsSnapshot() !== lastSavedGroupsSnapshot;
    updateSaveStatus();
}

function markSavedSnapshot() {
    lastSavedGroupsSnapshot = getGroupsSnapshot();
    hasUnsavedChanges = false;
    updateSaveStatus();
}

function renderItemPreview(preview, itemId) {
    const cleanId = (itemId || '').trim();
    const item = cleanId ? items.find(i => i.id === cleanId) : null;

    if (!cleanId) {
        preview.innerHTML = `
            <div class="preview-icon">--</div>
            <div class="preview-info">
                <div class="preview-name">未选择物品</div>
                <div class="preview-id">请选择或搜索物品</div>
            </div>
        `;
        preview.classList.add('show');
        return;
    }

    if (!item) {
        preview.innerHTML = `
            <div class="preview-icon">${escapeHtml(cleanId.split(':')[1]?.substring(0, 4) || '??')}</div>
            <div class="preview-info">
                <div class="preview-name">未知物品</div>
                <div class="preview-id">${escapeHtml(cleanId)}</div>
            </div>
        `;
        preview.classList.add('show');
        return;
    }

    preview.innerHTML = `
        <div class="preview-icon">${escapeHtml(cleanId.split(':')[1]?.substring(0, 4) || '??')}</div>
        <div class="preview-info">
            <div class="preview-name">${escapeHtml(item.name || cleanId)}</div>
            <div class="preview-id">${escapeHtml(cleanId)}</div>
        </div>
    `;
    preview.classList.add('show');
}

document.addEventListener('DOMContentLoaded', async () => {
    await loadItems();
    await loadTrades();
    setupEventListeners();
});

async function loadItems() {
    try {
        const res = await fetch('/api/items');
        items = await res.json();
    } catch (e) {
        console.error('Failed to load items:', e);
    }
}

async function loadTrades() {
    try {
        const res = await fetch('/api/trades');
        groups = await res.json();
        renderGroups();
        renderTrades();
        markSavedSnapshot();
    } catch (e) {
        console.error('Failed to load trades:', e);
    }
}

function setupEventListeners() {
    document.getElementById('saveBtn').addEventListener('click', saveConfig);
    document.getElementById('addGroupBtn').addEventListener('click', addGroup);
    document.getElementById('addTradeBtn').addEventListener('click', () => showModal(null));

    document.querySelectorAll('.count-adjust-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.dataset.target;
            const step = parseInt(btn.dataset.step, 10) || 0;
            const input = document.getElementById(targetId);
            if (!input || step === 0) {
                return;
            }

            const min = parseInt(input.min, 10) || 1;
            const max = parseInt(input.max, 10) || 64;
            const current = parseInt(input.value, 10) || min;
            const next = Math.max(min, Math.min(max, current + step));
            input.value = String(next);
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
            input.focus();
        });
    });
    document.getElementById('closeModal').addEventListener('click', hideModal);
    document.getElementById('cancelBtn').addEventListener('click', hideModal);
    document.getElementById('modalOverlay').addEventListener('click', (e) => {
        if (e.target.id === 'modalOverlay') hideModal();
    });
    document.getElementById('tradeForm').addEventListener('submit', saveTrade);
    document.getElementById('deleteTradeBtn').addEventListener('click', deleteTrade);

    setupItemSearch('inputItem', 'inputItemSuggestions');
    setupItemSearch('outputItem', 'outputItemSuggestions');

    // NBT modal buttons
    document.getElementById('inputNbtBtn').addEventListener('click', () => showNbtModal('input'));
    document.getElementById('outputNbtBtn').addEventListener('click', () => showNbtModal('output'));
    document.getElementById('closeNbtModal').addEventListener('click', hideNbtModal);
    document.getElementById('nbtCancelBtn').addEventListener('click', hideNbtModal);
    document.getElementById('nbtModalOverlay').addEventListener('click', (e) => {
        if (e.target.id === 'nbtModalOverlay') hideNbtModal();
    });
    document.getElementById('nbtConfirmBtn').addEventListener('click', confirmNbt);

    // NBT quick actions
    document.getElementById('nbtClearBtn').addEventListener('click', () => {
        document.getElementById('nbtTextarea').value = '';
    });
    document.getElementById('nbtCustomNameBtn').addEventListener('click', () => {
        const textarea = document.getElementById('nbtTextarea');
        appendNbt(textarea, '{"display":{"Name":"{\\"text\\":\\"请修改名称\\",\\"color\\":\\"gold\\"}"}}');
    });

    initNbtPresetSelector();

    window.addEventListener('beforeunload', (e) => {
        if (!hasUnsavedChanges) {
            return;
        }
        e.preventDefault();
        e.returnValue = '还有未保存的提交';
    });
}

function dedupeNbtArray(items) {
    const seen = new Set();
    return items.filter(item => {
        const key = (item && typeof item === 'object')
            ? JSON.stringify(item)
            : `__primitive:${String(item)}`;
        if (seen.has(key)) {
            return false;
        }
        seen.add(key);
        return true;
    });
}

function mergeNbtObject(target, source) {
    Object.entries(source).forEach(([key, value]) => {
        const current = target[key];

        if (Array.isArray(value)) {
            if (Array.isArray(current)) {
                target[key] = dedupeNbtArray(current.concat(value));
            } else {
                target[key] = dedupeNbtArray(value);
            }
            return;
        }

        if (value && typeof value === 'object') {
            if (current && typeof current === 'object' && !Array.isArray(current)) {
                mergeNbtObject(current, value);
            } else {
                target[key] = value;
            }
            return;
        }

        target[key] = value;
    });
}

function appendNbt(textarea, newNbtStr) {
    const currentNbt = textarea.value.trim();
    if (!currentNbt) {
        textarea.value = newNbtStr;
        return;
    }

    try {
        const currentObj = JSON.parse(currentNbt);
        const newObj = JSON.parse(newNbtStr);
        mergeNbtObject(currentObj, newObj);
        textarea.value = JSON.stringify(currentObj);
    } catch (e) {
        textarea.value = newNbtStr;
    }
}

function renderNbtPresetButtons() {
    const categorySelect = document.getElementById('nbtPresetCategory');
    const container = document.getElementById('nbtPresetList');
    if (!categorySelect || !container) {
        return;
    }

    const category = NBT_PRESET_CATEGORIES.find(item => item.key === categorySelect.value) || NBT_PRESET_CATEGORIES[0];
    container.innerHTML = category.presets.map(preset => {
        const nbtJson = JSON.stringify(preset.nbt);
        return `<button type="button" class="nbt-template-btn" data-nbt='${escapeHtml(nbtJson)}'>${escapeHtml(preset.label)}</button>`;
    }).join('');

    container.querySelectorAll('.nbt-template-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const nbt = btn.dataset.nbt;
            const textarea = document.getElementById('nbtTextarea');
            appendNbt(textarea, nbt);
        });
    });
}

function initNbtPresetSelector() {
    const categorySelect = document.getElementById('nbtPresetCategory');
    if (!categorySelect) {
        return;
    }

    categorySelect.innerHTML = NBT_PRESET_CATEGORIES.map(category => {
        return `<option value="${escapeHtml(category.key)}">${escapeHtml(category.label)}</option>`;
    }).join('');

    categorySelect.addEventListener('change', renderNbtPresetButtons);
    categorySelect.value = NBT_PRESET_CATEGORIES[0].key;
    renderNbtPresetButtons();
}

function setupItemSearch(inputId, suggestionsId) {
    const input = document.getElementById(inputId);
    const suggestions = document.getElementById(suggestionsId);
    const previewId = inputId.replace('Item', 'ItemPreview');
    const preview = document.getElementById(previewId);
    let selectedIndex = -1;
    let displayedItems = [];
    const MAX_DISPLAY = 100;

    input.addEventListener('focus', () => {
        showSuggestions('');
    });

    input.addEventListener('input', () => {
        const query = input.value.toLowerCase();
        showSuggestions(query);
    });

    function showSuggestions(query) {

        let filtered = items;
        if (query.length > 0) {
            filtered = items.filter(item => {
                const id = item.id.toLowerCase();
                const name = (item.name || '').toLowerCase();
                const translationKey = (item.translationKey || '').toLowerCase();
                return id.includes(query) || name.includes(query) || translationKey.includes(query);
            });
        }

        debugLog('搜索结果:', filtered.length, '条', query ? `关键词: "${query}"` : '(全部)');

        displayedItems = filtered.slice(0, MAX_DISPLAY);

        if (displayedItems.length === 0) {
            suggestions.classList.remove('show');
            return;
        }

        const hasMore = filtered.length > MAX_DISPLAY;

        suggestions.innerHTML = displayedItems.map((item, index) => `
            <div class="suggestion-item ${index === selectedIndex ? 'selected' : ''}" data-index="${index}" data-id="${escapeHtml(item.id)}">
                <div class="item-icon">${escapeHtml(getItemIconText(item))}</div>
                <div class="item-info">
                    <div class="item-name">${escapeHtml(item.name || item.id)}</div>
                    <div class="item-id">${escapeHtml(item.id)}</div>
                </div>
            </div>
        `).join('');

        if (hasMore) {
            suggestions.innerHTML += `<div class="load-more" tabindex="-1" aria-hidden="true">已展示前${MAX_DISPLAY}项（共${filtered.length}项）</div>`;
        }

        // Position suggestions directly below input
        const inputRect = input.getBoundingClientRect();
        const formGroup = input.closest('.form-group');
        const formGroupRect = formGroup.getBoundingClientRect();
        suggestions.style.top = `${inputRect.bottom - formGroupRect.top}px`;
        suggestions.style.left = `${inputRect.left - formGroupRect.left}px`;
        suggestions.style.width = `${inputRect.width}px`;

        suggestions.classList.add('show');
        selectedIndex = -1;

        suggestions.querySelectorAll('.suggestion-item').forEach(el => {
            el.addEventListener('click', () => {
                selectItem(el.dataset.id);
            });
        });
    }

    function getItemIconText(item) {
        // Extract item name from ID (e.g., "minecraft:diamond_sword" -> "DS")
        const parts = item.id.split(':');
        const name = parts[parts.length - 1];
        const words = name.split('_');
        if (words.length >= 2) {
            return (words[0][0] + words[1][0]).toUpperCase();
        }
        return name.substring(0, 2).toUpperCase();
    }

    function selectItem(itemId) {
        input.value = itemId;
        suggestions.classList.remove('show');
        updatePreview(itemId);
    }

    function updatePreview(itemId) {
        renderItemPreview(preview, itemId);
    }

    input.addEventListener('blur', (e) => {
        // Don't hide if clicking inside suggestions
        setTimeout(() => {
            if (!suggestions.contains(document.activeElement)) {
                suggestions.classList.remove('show');
                // Update preview if input has a valid item ID
                if (input.value) {
                    updatePreview(input.value);
                }
            }
        }, 150);
    });

    input.addEventListener('keydown', (e) => {
        const items_list = suggestions.querySelectorAll('.suggestion-item');
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            selectedIndex = Math.min(selectedIndex + 1, items_list.length - 1);
            updateSelection(items_list);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            selectedIndex = Math.max(selectedIndex - 1, 0);
            updateSelection(items_list);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (selectedIndex >= 0 && items_list[selectedIndex]) {
                selectItem(items_list[selectedIndex].dataset.id);
            }
        } else if (e.key === 'Escape') {
            suggestions.classList.remove('show');
        }
    });

    function updateSelection(items_list) {
        items_list.forEach((el, i) => {
            el.classList.toggle('selected', i === selectedIndex);
        });
        if (selectedIndex >= 0 && items_list[selectedIndex]) {
            items_list[selectedIndex].scrollIntoView({ block: 'nearest' });
        }
    }
}

async function validateNbt(inputId, errorId) {
    const nbt = document.getElementById(inputId).value;
    const errorEl = document.getElementById(errorId);

    if (!nbt.trim()) {
        errorEl.textContent = '';
        return true;
    }

    try {
        const res = await fetch(`/api/nbt/validate?nbt=${encodeURIComponent(nbt)}`);
        const data = await res.json();

        if (data.valid) {
            errorEl.textContent = '';
            return true;
        } else {
            errorEl.textContent = data.error || 'Invalid NBT';
            return false;
        }
    } catch (e) {
        errorEl.textContent = 'Validation failed';
        return false;
    }
}

function clearGroupDropIndicators(container) {
    container.classList.remove('drop-at-start', 'drop-at-end');
    container.querySelectorAll('.group-item').forEach(item => {
        item.classList.remove('drag-over', 'drop-before', 'drop-after');
    });
}

function getGroupInsertIndexByPointer(container, clientY) {
    const groupItems = Array.from(container.querySelectorAll('.group-item'));
    for (let i = 0; i < groupItems.length; i += 1) {
        const rect = groupItems[i].getBoundingClientRect();
        if (clientY < rect.top + rect.height / 2) {
            return i;
        }
    }
    return groupItems.length;
}

function clearTradeDropIndicators(container) {
    container.classList.remove('drop-at-start', 'drop-at-end');
    container.querySelectorAll('.trade-item').forEach(item => {
        item.classList.remove('drag-over', 'drop-before', 'drop-after');
    });
}

function getTradeInsertIndexByPointer(container, clientY) {
    const tradeItems = Array.from(container.querySelectorAll('.trade-item'));
    for (let i = 0; i < tradeItems.length; i += 1) {
        const rect = tradeItems[i].getBoundingClientRect();
        if (clientY < rect.top + rect.height / 2) {
            return i;
        }
    }
    return tradeItems.length;
}

function renderGroups() {
    const container = document.getElementById('groupsList');
    container.innerHTML = groups.map((group, index) => `
        <div class="group-item ${index === selectedGroupIndex ? 'active' : ''}" data-index="${index}">
            <div class="group-main">
                <button class="drag-handle" title="拖拽排序" draggable="true" aria-label="拖拽排序">⋮⋮</button>
                <span class="group-name">${escapeHtml(group.group)}</span>
            </div>
            <div class="group-actions">
                <button class="edit-btn" data-action="edit" title="编辑名称">✏️</button>
                <button class="delete-btn" data-action="delete" title="删除分组">×</button>
            </div>
        </div>
    `).join('');

    container.ondragover = (e) => {
        if (draggingGroupIndex < 0) {
            return;
        }
        if (e.target.closest('.group-item')) {
            return;
        }

        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';

        clearGroupDropIndicators(container);
        const insertIndex = getGroupInsertIndexByPointer(container, e.clientY);

        if (insertIndex <= 0) {
            container.classList.add('drop-at-start');
            return;
        }

        if (insertIndex >= groups.length) {
            container.classList.add('drop-at-end');
            return;
        }

        const targetItem = container.querySelector(`.group-item[data-index="${insertIndex}"]`);
        if (targetItem) {
            targetItem.classList.add('drag-over', 'drop-before');
        }
    };

    container.ondragleave = (e) => {
        if (container.contains(e.relatedTarget)) {
            return;
        }
        clearGroupDropIndicators(container);
    };

    container.ondrop = (e) => {
        if (draggingGroupIndex < 0) {
            return;
        }
        if (e.target.closest('.group-item')) {
            return;
        }

        e.preventDefault();
        const insertIndex = getGroupInsertIndexByPointer(container, e.clientY);
        moveGroupTo(draggingGroupIndex, insertIndex);
        draggingGroupIndex = -1;
        clearGroupDropIndicators(container);
    };

    container.querySelectorAll('.group-item').forEach(el => {
        const index = parseInt(el.dataset.index, 10);
        const dragHandle = el.querySelector('.drag-handle');

        el.addEventListener('click', (e) => {
            if (e.target.classList.contains('drag-handle')) {
                return;
            }
            if (e.target.classList.contains('delete-btn')) {
                deleteGroup(index);
                return;
            }
            if (e.target.classList.contains('edit-btn')) {
                const newName = prompt('输入新的分组名称:', groups[index].group);
                if (newName && newName.trim()) {
                    groups[index].group = newName.trim();
                    renderGroups();
                    if (selectedGroupIndex === index) {
                        document.getElementById('currentGroupName').textContent = newName.trim();
                    }
                    refreshUnsavedChangesState();
                }
                return;
            }
            selectGroup(index);
        });

        dragHandle.addEventListener('dragstart', (e) => {
            draggingGroupIndex = index;
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', String(index));
            el.classList.add('dragging');
        });

        dragHandle.addEventListener('dragend', () => {
            draggingGroupIndex = -1;
            clearGroupDropIndicators(container);
            container.querySelectorAll('.group-item').forEach(item => item.classList.remove('dragging'));
        });

        el.addEventListener('dragover', (e) => {
            if (draggingGroupIndex < 0 || draggingGroupIndex === index) {
                return;
            }
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';

            const rect = el.getBoundingClientRect();
            const isAfter = (e.clientY - rect.top) > rect.height / 2;

            clearGroupDropIndicators(container);
            el.classList.add('drag-over', isAfter ? 'drop-after' : 'drop-before');
        });

        el.addEventListener('dragleave', (e) => {
            if (el.contains(e.relatedTarget)) {
                return;
            }
            el.classList.remove('drag-over', 'drop-before', 'drop-after');
        });

        el.addEventListener('drop', (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (draggingGroupIndex < 0 || draggingGroupIndex === index) {
                clearGroupDropIndicators(container);
                return;
            }

            const rect = el.getBoundingClientRect();
            const isAfter = (e.clientY - rect.top) > rect.height / 2;
            const insertIndex = isAfter ? index + 1 : index;

            moveGroupTo(draggingGroupIndex, insertIndex);
            draggingGroupIndex = -1;
            clearGroupDropIndicators(container);
        });
    });
}

function selectGroup(index) {
    selectedGroupIndex = index;
    selectedTradeIndex = -1;
    renderGroups();
    renderTrades();
}

function renderTrades() {
    const panel = document.getElementById('tradesPanel');
    const container = document.getElementById('tradesList');
    const nameEl = document.getElementById('currentGroupName');

    if (selectedGroupIndex < 0 || selectedGroupIndex >= groups.length) {
        panel.style.display = 'none';
        return;
    }

    panel.style.display = 'block';
    const group = groups[selectedGroupIndex];
    nameEl.textContent = group.group;

    if (!group.trades || group.trades.length === 0) {
        container.innerHTML = '<div class="hint">暂无交易，点击上方"+ 新建交易"添加</div>';
        return;
    }

    container.innerHTML = group.trades.map((trade, index) => {
        const xp = Math.max(0, parseInt(trade.xpReward, 10) || 0);
        const hasInputNbt = Boolean(trade.inputNbt && String(trade.inputNbt).trim());
        const hasOutputNbt = Boolean(trade.outputNbt && String(trade.outputNbt).trim());
        return `
        <div class="trade-item ${index === selectedTradeIndex ? 'selected' : ''}" data-index="${index}">
            <div class="trade-main">
                <button class="drag-handle" title="拖拽排序" draggable="true" aria-label="拖拽排序">⋮⋮</button>
                <div class="trade-info">
                    <span class="trade-io">${trade.inputCount}× ${escapeHtml(getItemDisplayName(trade.input))}${hasInputNbt ? ' <span class="trade-nbt-tag">NBT</span>' : ''}</span>
                    <span class="trade-arrow">→</span>
                    <span class="trade-io">${trade.outputCount}× ${escapeHtml(getItemDisplayName(trade.output))}${hasOutputNbt ? ' <span class="trade-nbt-tag">NBT</span>' : ''}</span>
                    <span class="trade-xp">XP +${xp}</span>
                </div>
            </div>
            <div class="trade-actions">
                <button class="delete-btn" data-action="delete">×</button>
            </div>
        </div>
    `;
    }).join('');

    container.ondragover = (e) => {
        if (draggingTradeIndex < 0) {
            return;
        }
        if (e.target.closest('.trade-item')) {
            return;
        }

        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';

        clearTradeDropIndicators(container);
        const insertIndex = getTradeInsertIndexByPointer(container, e.clientY);

        if (insertIndex <= 0) {
            container.classList.add('drop-at-start');
            return;
        }

        if (insertIndex >= group.trades.length) {
            container.classList.add('drop-at-end');
            return;
        }

        const targetItem = container.querySelector(`.trade-item[data-index="${insertIndex}"]`);
        if (targetItem) {
            targetItem.classList.add('drag-over', 'drop-before');
        }
    };

    container.ondragleave = (e) => {
        if (container.contains(e.relatedTarget)) {
            return;
        }
        clearTradeDropIndicators(container);
    };

    container.ondrop = (e) => {
        if (draggingTradeIndex < 0) {
            return;
        }
        if (e.target.closest('.trade-item')) {
            return;
        }

        e.preventDefault();
        const insertIndex = getTradeInsertIndexByPointer(container, e.clientY);
        moveTradeTo(draggingTradeIndex, insertIndex);
        draggingTradeIndex = -1;
        clearTradeDropIndicators(container);
    };

    container.querySelectorAll('.trade-item').forEach(el => {
        const index = parseInt(el.dataset.index, 10);
        const dragHandle = el.querySelector('.drag-handle');

        el.addEventListener('click', (e) => {
            if (e.target.classList.contains('delete-btn') || e.target.classList.contains('drag-handle')) return;
            editTrade(parseInt(el.dataset.index, 10));
        });

        dragHandle.addEventListener('dragstart', (e) => {
            draggingTradeIndex = index;
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', String(index));
            el.classList.add('dragging');
        });

        dragHandle.addEventListener('dragend', () => {
            draggingTradeIndex = -1;
            clearTradeDropIndicators(container);
            container.querySelectorAll('.trade-item').forEach(item => item.classList.remove('dragging'));
        });

        el.addEventListener('dragover', (e) => {
            if (draggingTradeIndex < 0 || draggingTradeIndex === index) {
                return;
            }
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';

            const rect = el.getBoundingClientRect();
            const isAfter = (e.clientY - rect.top) > rect.height / 2;

            clearTradeDropIndicators(container);
            el.classList.add('drag-over', isAfter ? 'drop-after' : 'drop-before');
        });

        el.addEventListener('dragleave', (e) => {
            if (el.contains(e.relatedTarget)) {
                return;
            }
            el.classList.remove('drag-over', 'drop-before', 'drop-after');
        });

        el.addEventListener('drop', (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (draggingTradeIndex < 0 || draggingTradeIndex === index) {
                clearTradeDropIndicators(container);
                return;
            }

            const rect = el.getBoundingClientRect();
            const isAfter = (e.clientY - rect.top) > rect.height / 2;
            const insertIndex = isAfter ? index + 1 : index;

            moveTradeTo(draggingTradeIndex, insertIndex);
            draggingTradeIndex = -1;
            clearTradeDropIndicators(container);
        });

        el.querySelector('.delete-btn').addEventListener('click', () => {
            const tradeIndex = parseInt(el.dataset.index, 10);
            if (!Number.isInteger(tradeIndex)) {
                return;
            }
            if (!confirm('确认删除这条交易吗？')) {
                return;
            }
            deleteTradeFromList(tradeIndex);
        });
    });
}

function getItemName(itemId) {
    const item = items.find(i => i.id === itemId);
    return item ? (item.name || itemId) : itemId;
}

function getItemDisplayName(itemId) {
    const item = items.find(i => i.id === itemId);
    if (!item) return itemId;
    return item.name || itemId;
}

function addGroup() {
    groups.push({ group: '新分组', trades: [] });
    selectedGroupIndex = groups.length - 1;
    renderGroups();
    renderTrades();
    refreshUnsavedChangesState();
}

function moveGroupTo(fromIndex, insertIndex) {
    if (fromIndex < 0 || fromIndex >= groups.length || insertIndex < 0 || insertIndex > groups.length) {
        return;
    }

    if (insertIndex === fromIndex || insertIndex === fromIndex + 1) {
        return;
    }

    const selectedGroup = selectedGroupIndex >= 0 ? groups[selectedGroupIndex] : null;
    const [moved] = groups.splice(fromIndex, 1);

    if (fromIndex < insertIndex) {
        insertIndex -= 1;
    }

    groups.splice(insertIndex, 0, moved);

    if (selectedGroup) {
        selectedGroupIndex = groups.indexOf(selectedGroup);
    }

    renderGroups();
    renderTrades();
    refreshUnsavedChangesState();
}

function moveTradeTo(fromIndex, insertIndex) {
    if (selectedGroupIndex < 0 || selectedGroupIndex >= groups.length) {
        return;
    }

    const trades = groups[selectedGroupIndex].trades;
    if (!Array.isArray(trades) || fromIndex < 0 || fromIndex >= trades.length || insertIndex < 0 || insertIndex > trades.length) {
        return;
    }

    if (insertIndex === fromIndex || insertIndex === fromIndex + 1) {
        return;
    }

    const selectedTrade = selectedTradeIndex >= 0 ? trades[selectedTradeIndex] : null;
    const [moved] = trades.splice(fromIndex, 1);

    if (fromIndex < insertIndex) {
        insertIndex -= 1;
    }

    trades.splice(insertIndex, 0, moved);

    if (selectedTrade) {
        selectedTradeIndex = trades.indexOf(selectedTrade);
    }

    renderTrades();
    refreshUnsavedChangesState();
}

function deleteGroup(index) {
    if (groups.length <= 1) {
        alert('至少保留一个分组');
        return;
    }
    groups.splice(index, 1);
    if (selectedGroupIndex >= groups.length) {
        selectedGroupIndex = groups.length - 1;
    }
    renderGroups();
    renderTrades();
    refreshUnsavedChangesState();
}

function showModal(trade) {
    const overlay = document.getElementById('modalOverlay');
    const title = document.getElementById('modalTitle');
    const deleteBtn = document.getElementById('deleteTradeBtn');

    overlay.classList.add('show');
    editingTrade = trade;

    if (trade) {
        title.textContent = '编辑交易';
        document.getElementById('inputItem').value = trade.input || '';
        document.getElementById('inputCount').value = trade.inputCount || 1;
        document.getElementById('inputNbt').value = trade.inputNbt || '';
        updateNbtPreview('input', trade.inputNbt || '');
        document.getElementById('outputItem').value = trade.output || '';
        document.getElementById('outputCount').value = trade.outputCount || 1;
        document.getElementById('outputNbt').value = trade.outputNbt || '';
        updateNbtPreview('output', trade.outputNbt || '');
        document.getElementById('nbtMatchMode').value = trade.nbtMatchMode || 'exact';
        document.getElementById('xpReward').value = trade.xpReward || 0;
        renderItemPreview(document.getElementById('inputItemPreview'), trade.input || '');
        renderItemPreview(document.getElementById('outputItemPreview'), trade.output || '');
        deleteBtn.style.display = 'none';
    } else {
        title.textContent = '新建交易';
        selectedTradeIndex = -1;
        document.getElementById('tradeForm').reset();
        document.getElementById('inputNbt').value = '';
        updateNbtPreview('input', '');
        document.getElementById('outputNbt').value = '';
        updateNbtPreview('output', '');
        renderItemPreview(document.getElementById('inputItemPreview'), '');
        renderItemPreview(document.getElementById('outputItemPreview'), '');
        deleteBtn.style.display = 'none';
    }

    document.getElementById('inputNbtError').textContent = '';
    document.getElementById('outputNbtError').textContent = '';
}

function hideModal() {
    document.getElementById('modalOverlay').classList.remove('show');
    editingTrade = null;
}

function showNbtModal(target) {
    currentNbtTarget = target;
    const overlay = document.getElementById('nbtModalOverlay');
    const title = document.getElementById('nbtModalTitle');
    const textarea = document.getElementById('nbtTextarea');

    title.textContent = target === 'input' ? '编辑输入 NBT' : '编辑输出 NBT';
    textarea.value = document.getElementById(target + 'Nbt').value || '';
    document.getElementById('nbtTextareaError').textContent = '';

    overlay.classList.add('show');
}

function hideNbtModal() {
    document.getElementById('nbtModalOverlay').classList.remove('show');
    currentNbtTarget = null;
}

async function confirmNbt() {
    const nbt = document.getElementById('nbtTextarea').value.trim();

    if (!nbt) {
        // Empty is valid - means no NBT
        document.getElementById(currentNbtTarget + 'Nbt').value = '';
        updateNbtPreview(currentNbtTarget, '');
        hideNbtModal();
        return;
    }

    // Validate NBT
    try {
        const res = await fetch(`/api/nbt/validate?nbt=${encodeURIComponent(nbt)}`);
        const data = await res.json();

        if (data.valid) {
            document.getElementById(currentNbtTarget + 'Nbt').value = nbt;
            updateNbtPreview(currentNbtTarget, nbt);
            hideNbtModal();
        } else {
            document.getElementById('nbtTextareaError').textContent = data.error || 'Invalid NBT';
        }
    } catch (e) {
        document.getElementById('nbtTextareaError').textContent = 'Validation failed';
    }
}

function updateNbtPreview(target, nbt) {
    const preview = document.getElementById(target + 'NbtPreview');
    if (nbt && nbt.trim()) {
        preview.textContent = nbt;
        preview.classList.add('has-nbt');
    } else {
        preview.textContent = '未设置';
        preview.classList.remove('has-nbt');
    }
}

function editTrade(index) {
    selectedTradeIndex = index;
    renderTrades();
    const trade = groups[selectedGroupIndex].trades[index];
    showModal(trade);
}

async function saveTrade(e) {
    e.preventDefault();

    if (selectedGroupIndex < 0 || selectedGroupIndex >= groups.length) {
        alert('请先选择交易分组');
        return;
    }

    const inputNbtValid = await validateNbt('inputNbt', 'inputNbtError');
    const outputNbtValid = await validateNbt('outputNbt', 'outputNbtError');

    if (!inputNbtValid || !outputNbtValid) {
        return;
    }

    const inputCount = Math.max(1, Math.min(64, parseInt(document.getElementById('inputCount').value, 10) || 1));
    const outputCount = Math.max(1, Math.min(64, parseInt(document.getElementById('outputCount').value, 10) || 1));
    const xpReward = Math.max(0, parseInt(document.getElementById('xpReward').value, 10) || 0);

    const trade = {
        input: document.getElementById('inputItem').value.trim(),
        inputCount,
        inputNbt: document.getElementById('inputNbt').value || null,
        output: document.getElementById('outputItem').value.trim(),
        outputCount,
        outputNbt: document.getElementById('outputNbt').value || null,
        nbtMatchMode: document.getElementById('nbtMatchMode').value,
        xpReward
    };

    if (!trade.input || !trade.output) {
        alert('请选择输入和输出物品');
        return;
    }

    if (selectedTradeIndex >= 0) {
        groups[selectedGroupIndex].trades[selectedTradeIndex] = trade;
    } else {
        groups[selectedGroupIndex].trades.push(trade);
    }

    renderTrades();
    hideModal();
    refreshUnsavedChangesState();
}

function deleteTrade() {
    if (selectedTradeIndex < 0) return;
    deleteTradeFromList(selectedTradeIndex);
}

function deleteTradeFromList(index) {
    groups[selectedGroupIndex].trades.splice(index, 1);
    if (selectedTradeIndex === index) {
        hideModal();
        selectedTradeIndex = -1;
    }
    renderTrades();
    refreshUnsavedChangesState();
}

async function saveConfig() {
    try {
        const res = await fetch('/api/trades', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(groups)
        });

        let data = {};
        try {
            data = await res.json();
        } catch (ignored) {}

        if (!res.ok || !data.success) {
            alert('保存失败: ' + (data.error || `HTTP ${res.status}`));
            return;
        }

        markSavedSnapshot();
        alert('配置已保存');
    } catch (e) {
        alert('保存失败: ' + e.message);
    }
}
