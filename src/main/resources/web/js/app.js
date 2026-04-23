let items = [];
let groups = [];
let selectedGroupIndex = -1;
let selectedTradeIndex = -1;
let editingTrade = null;
let currentNbtTarget = null; // 'input' or 'output'
let draggingGroupIndex = -1;
const DEBUG = false;

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
    } catch (e) {
        console.error('Failed to load trades:', e);
    }
}

function setupEventListeners() {
    document.getElementById('saveBtn').addEventListener('click', saveConfig);
    document.getElementById('addGroupBtn').addEventListener('click', addGroup);
    document.getElementById('addTradeBtn').addEventListener('click', () => showModal(null));
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
        document.getElementById('nbtTextarea').value = '{"display":{"Name":"{\\"text\\":\\"请修改名称\\",\\"color\\":\\"gold\\"}"}}';
    });

    // NBT template buttons
    document.querySelectorAll('.nbt-template-grid .nbt-template-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const nbt = btn.dataset.nbt;
            const textarea = document.getElementById('nbtTextarea');
            appendNbt(textarea, nbt);
        });
    });
}

function appendNbt(textarea, newNbtStr) {
    let currentNbt = textarea.value.trim();
    if (!currentNbt) {
        textarea.value = newNbtStr;
        return;
    }

    try {
        // Parse both NBT strings
        let currentObj = JSON.parse(currentNbt);
        let newObj = JSON.parse(newNbtStr);

        // Merge enchantments
        if (newObj.Enchantments && currentObj.Enchantments) {
            // Append new enchantments to existing
            currentObj.Enchantments = currentObj.Enchantments.concat(newObj.Enchantments);
        } else if (newObj.Enchantments) {
            currentObj.Enchantments = newObj.Enchantments;
        }

        if (newObj.StoredEnchantments && currentObj.StoredEnchantments) {
            currentObj.StoredEnchantments = currentObj.StoredEnchantments.concat(newObj.StoredEnchantments);
        } else if (newObj.StoredEnchantments) {
            currentObj.StoredEnchantments = newObj.StoredEnchantments;
        }

        if (newObj.display && !currentObj.display) {
            currentObj.display = newObj.display;
        }

        textarea.value = JSON.stringify(currentObj);
    } catch (e) {
        // If parsing fails, just replace
        textarea.value = newNbtStr;
    }
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
            suggestions.innerHTML += `<div class="load-more" tabindex="-1" aria-hidden="true">已展示前${MAX_DISPLAY}项，请缩小关键词继续搜索（共${filtered.length}项）</div>`;
        }

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

    container.querySelectorAll('.group-item').forEach(el => {
        const index = parseInt(el.dataset.index);
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
                }
                return;
            }
            selectGroup(index);
        });

        dragHandle.addEventListener('dragstart', (e) => {
            draggingGroupIndex = index;
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', String(index));
        });

        dragHandle.addEventListener('dragend', () => {
            draggingGroupIndex = -1;
            container.querySelectorAll('.group-item').forEach(item => item.classList.remove('drag-over'));
        });

        el.addEventListener('dragover', (e) => {
            if (draggingGroupIndex < 0 || draggingGroupIndex === index) {
                return;
            }
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            el.classList.add('drag-over');
        });

        el.addEventListener('dragleave', () => {
            el.classList.remove('drag-over');
        });

        el.addEventListener('drop', (e) => {
            e.preventDefault();
            el.classList.remove('drag-over');
            if (draggingGroupIndex < 0 || draggingGroupIndex === index) {
                return;
            }
            moveGroupTo(draggingGroupIndex, index);
            draggingGroupIndex = -1;
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
        return `
        <div class="trade-item ${index === selectedTradeIndex ? 'selected' : ''}" data-index="${index}">
            <div class="trade-info">
                <span>${trade.inputCount}× ${escapeHtml(getItemDisplayName(trade.input))}</span>
                <span class="trade-arrow">→</span>
                <span>${trade.outputCount}× ${escapeHtml(getItemDisplayName(trade.output))}</span>
                <span class="trade-xp">XP +${xp}</span>
            </div>
            <button class="delete-btn" data-action="delete">×</button>
        </div>
    `;
    }).join('');

    container.querySelectorAll('.trade-item').forEach(el => {
        el.addEventListener('click', (e) => {
            if (e.target.classList.contains('delete-btn')) return;
            editTrade(parseInt(el.dataset.index));
        });

        el.querySelector('.delete-btn').addEventListener('click', () => {
            const index = parseInt(el.dataset.index);
            if (!Number.isInteger(index)) {
                return;
            }
            if (!confirm('确认删除这条交易吗？')) {
                return;
            }
            deleteTradeFromList(index);
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
}

function moveGroupTo(fromIndex, toIndex) {
    if (fromIndex < 0 || fromIndex >= groups.length || toIndex < 0 || toIndex >= groups.length || fromIndex === toIndex) {
        return;
    }

    const [moved] = groups.splice(fromIndex, 1);
    groups.splice(toIndex, 0, moved);

    if (selectedGroupIndex === fromIndex) {
        selectedGroupIndex = toIndex;
    } else if (fromIndex < toIndex && selectedGroupIndex > fromIndex && selectedGroupIndex <= toIndex) {
        selectedGroupIndex -= 1;
    } else if (fromIndex > toIndex && selectedGroupIndex >= toIndex && selectedGroupIndex < fromIndex) {
        selectedGroupIndex += 1;
    }

    renderGroups();
    renderTrades();
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

        alert('配置已保存');
    } catch (e) {
        alert('保存失败: ' + e.message);
    }
}
