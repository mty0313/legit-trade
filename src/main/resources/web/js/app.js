let items = [];
let groups = [];
let selectedGroupIndex = -1;
let selectedTradeIndex = -1;
let editingTrade = null;
let currentNbtTarget = null; // 'input' or 'output'

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
    let loadedCount = 0;
    const BATCH_SIZE = 50;

    input.addEventListener('focus', () => {
        // Show first batch on focus
        loadedCount = 0;
        showSuggestions('', true);
    });

    input.addEventListener('input', () => {
        const query = input.value.toLowerCase();
        loadedCount = 0;
        showSuggestions(query, true);
    });

    function showSuggestions(query, reset = false) {
        if (reset) {
            loadedCount = 0;
        }

        let filtered = items;
        if (query.length > 0) {
            filtered = items.filter(item => {
                const id = item.id.toLowerCase();
                const name = (item.name || '').toLowerCase();
                const translationKey = (item.translationKey || '').toLowerCase();
                return id.includes(query) || name.includes(query) || translationKey.includes(query);
            });
        }

        console.log('搜索结果:', filtered.length, '条', query ? `关键词: "${query}"` : '(全部)');
        console.log(JSON.stringify(filtered, null, 2));

        displayedItems = filtered;
        const batch = filtered.slice(loadedCount, loadedCount + BATCH_SIZE);

        if (batch.length === 0 && loadedCount === 0) {
            suggestions.classList.remove('show');
            return;
        }

        const hasMore = filtered.length > loadedCount + BATCH_SIZE;

        suggestions.innerHTML = batch.map((item, index) => `
            <div class="suggestion-item ${loadedCount + index === selectedIndex ? 'selected' : ''}" data-index="${loadedCount + index}" data-id="${item.id}">
                <div class="item-icon">${getItemIconText(item)}</div>
                <div class="item-info">
                    <div class="item-name">${item.name || item.id}</div>
                    <div class="item-id">${item.id}</div>
                </div>
            </div>
        `).join('');

        if (hasMore) {
            suggestions.innerHTML += `<div class="load-more" tabindex="0" data-count="${filtered.length}">加载更多 (${loadedCount + BATCH_SIZE}/${filtered.length})...</div>`;
        }

        suggestions.classList.add('show');
        selectedIndex = -1;

        suggestions.querySelectorAll('.suggestion-item').forEach(el => {
            el.addEventListener('click', () => {
                selectItem(el.dataset.id);
            });
        });

        const loadMoreBtn = suggestions.querySelector('.load-more');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('mousedown', (e) => {
                e.preventDefault(); // Prevent blur
            });
            loadMoreBtn.addEventListener('click', (e) => {
                e.preventDefault();
                loadedCount += BATCH_SIZE;
                renderBatch(filtered, query);
            });
        }
    }

    function renderBatch(filtered, query) {
        const batch = filtered.slice(loadedCount, loadedCount + BATCH_SIZE);
        const hasMore = filtered.length > loadedCount + BATCH_SIZE;

        suggestions.innerHTML = batch.map((item, index) => `
            <div class="suggestion-item ${loadedCount + index === selectedIndex ? 'selected' : ''}" data-index="${loadedCount + index}" data-id="${item.id}">
                <div class="item-icon">${getItemIconText(item)}</div>
                <div class="item-info">
                    <div class="item-name">${item.name || item.id}</div>
                    <div class="item-id">${item.id}</div>
                </div>
            </div>
        `).join('');

        if (hasMore) {
            suggestions.innerHTML += `<div class="load-more" tabindex="0" data-count="${filtered.length}">加载更多 (${loadedCount + BATCH_SIZE}/${filtered.length})...</div>`;
        }

        suggestions.querySelectorAll('.suggestion-item').forEach(el => {
            el.addEventListener('click', () => {
                selectItem(el.dataset.id);
            });
        });

        const loadMoreBtn = suggestions.querySelector('.load-more');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('mousedown', (e) => {
                e.preventDefault();
            });
            loadMoreBtn.addEventListener('click', (e) => {
                e.preventDefault();
                loadedCount += BATCH_SIZE;
                renderBatch(filtered, query);
            });
        }
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
        const item = items.find(i => i.id === itemId);
        if (!item) {
            preview.classList.remove('show');
            return;
        }

        preview.innerHTML = `
            <div class="preview-icon">${itemId.split(':')[1]?.substring(0, 4) || '??'}</div>
            <div class="preview-info">
                <div class="preview-name">${item.name || itemId}</div>
                <div class="preview-id">${itemId}</div>
            </div>
        `;
        preview.classList.add('show');
    }

    input.addEventListener('blur', (e) => {
        // Don't hide if clicking inside suggestions (load more button)
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
            <span class="group-name">${group.group}</span>
            <div class="group-actions">
                <button class="edit-btn" data-action="edit" title="编辑名称">✏️</button>
                <button class="delete-btn" data-action="delete" title="删除分组">×</button>
            </div>
        </div>
    `).join('');

    container.querySelectorAll('.group-item').forEach(el => {
        el.addEventListener('click', (e) => {
            if (e.target.classList.contains('delete-btn')) {
                deleteGroup(parseInt(el.dataset.index));
                return;
            }
            if (e.target.classList.contains('edit-btn')) {
                const index = parseInt(el.dataset.index);
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
            selectGroup(parseInt(el.dataset.index));
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

    container.innerHTML = group.trades.map((trade, index) => `
        <div class="trade-item ${index === selectedTradeIndex ? 'selected' : ''}" data-index="${index}">
            <div class="trade-info">
                <span>${trade.inputCount}× ${getItemDisplayName(trade.input)}</span>
                <span class="trade-arrow">→</span>
                <span>${trade.outputCount}× ${getItemDisplayName(trade.output)}</span>
            </div>
            <button class="delete-btn" data-action="delete">×</button>
        </div>
    `).join('');

    container.querySelectorAll('.trade-item').forEach(el => {
        el.addEventListener('click', (e) => {
            if (e.target.classList.contains('delete-btn')) return;
            editTrade(parseInt(el.dataset.index));
        });

        el.querySelector('.delete-btn').addEventListener('click', () => {
            deleteTradeFromList(parseInt(el.dataset.index));
        });
    });

    hideModal();
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
        deleteBtn.style.display = 'inline-block';
    } else {
        title.textContent = '新建交易';
        if (selectedGroupIndex >= 0) {
            selectedTradeIndex = -1;
            renderTrades();
        }
        document.getElementById('tradeForm').reset();
        document.getElementById('inputNbt').value = '';
        updateNbtPreview('input', '');
        document.getElementById('outputNbt').value = '';
        updateNbtPreview('output', '');
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

    const inputNbtValid = await validateNbt('inputNbt', 'inputNbtError');
    const outputNbtValid = await validateNbt('outputNbt', 'outputNbtError');

    if (!inputNbtValid || !outputNbtValid) {
        return;
    }

    const trade = {
        input: document.getElementById('inputItem').value,
        inputCount: parseInt(document.getElementById('inputCount').value) || 1,
        inputNbt: document.getElementById('inputNbt').value || null,
        output: document.getElementById('outputItem').value,
        outputCount: parseInt(document.getElementById('outputCount').value) || 1,
        outputNbt: document.getElementById('outputNbt').value || null,
        nbtMatchMode: document.getElementById('nbtMatchMode').value,
        xpReward: parseInt(document.getElementById('xpReward').value) || 0
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

        const data = await res.json();
        if (data.success) {
            alert('配置已保存');
        } else {
            alert('保存失败: ' + (data.error || '未知错误'));
        }
    } catch (e) {
        alert('保存失败: ' + e.message);
    }
}
