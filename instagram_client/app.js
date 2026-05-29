// Configuração Padrão (Vazio = mesmo host do frontend)
const DEFAULT_API_URL = '';

// Gerenciamento de Estado
const state = {
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    apiUrl: localStorage.getItem('api_url') || DEFAULT_API_URL,
    currentPage: 'login',
    refreshTimer: null,
    credentials: JSON.parse(localStorage.getItem('credentials')) || null,
    adminEditingUser: null
};

// --- Utilitários ---

function decodeJWT(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerText = message;
    container.appendChild(toast);
    
    setTimeout(() => toast.classList.add('show'), 100);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function logDebug(type, title, data) {
    const content = document.getElementById('debug-content');
    if (!content) return;
    const entry = document.createElement('div');
    entry.className = `debug-entry ${type}`;
    const timestamp = new Date().toLocaleTimeString();
    entry.innerHTML = `[${timestamp}] <b>${title}</b>:\n${JSON.stringify(data, null, 2)}`;
    content.prepend(entry);
}

// --- Comunicação com Servidor ---

async function request(endpoint, options = {}) {
    // Remove barras duplicadas se a apiUrl terminar com / ou o endpoint começar com /
    const baseUrl = state.apiUrl.endsWith('/') ? state.apiUrl.slice(0, -1) : state.apiUrl;
    const path = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    const url = `${baseUrl}${path}`;
    
    const method = (options.method || 'GET').toUpperCase();
    const isPublic = (path === '/usuarios/login' && method === 'POST') ||
                     (path === '/usuarios' && method === 'POST');

    const defaultHeaders = {};

    // Only send Content-Type if request has a body
    if (options.body) {
        defaultHeaders['Content-Type'] = 'application/json';
    }

    // Only send Authorization if not public and token is present
    if (!isPublic && state.token) {
        defaultHeaders['Authorization'] = `Bearer ${state.token}`;
    }

    const fetchOptions = Object.assign({}, options, {
        headers: Object.assign({}, defaultHeaders, options.headers)
    });

    const requestLog = {
        url: url,
        method: method,
        headers: fetchOptions.headers,
        body: fetchOptions.body ? JSON.parse(fetchOptions.body) : null
    };

    console.log("REQUISIÇÃO ENVIADA:", requestLog);
    logDebug('req', `FETCH ${method} ${endpoint}`, requestLog.body || {});

    try {
        const response = await fetch(url, fetchOptions);
        const data = await response.json();
        
        console.log("RESPOSTA RECEBIDA:", {
            status: response.status,
            ok: response.ok,
            data: data
        });

        logDebug(response.ok ? 'res' : 'err', `RESPONSE ${endpoint}`, data);

        if (!response.ok) {
            // Automatically reset session on 401 Unauthorized or expired token on private routes
            if (response.status === 401 && !isPublic && path !== '/usuarios/logout') {
                logout();
            }
            throw data;
        }

        return data;
    } catch (error) {
        console.error("ERRO NA REQUISIÇÃO:", error);
        
        const isTokenExpired = error.codigo === "TOKEN_EXPIRADO" || error.status === 403 || error.status === 401;
        if (isTokenExpired) {
            console.log("Sessão expirada. Efetuando limpeza de sessão...");
            if (!isPublic && path !== '/usuarios/logout') {
                logout();
            }
        }
        
        showToast(error.mensagem || 'Erro na comunicação com o servidor', 'error');
        throw error;
    }
}

// --- Lógica de Autenticação e Sessão ---

async function login(usuario, senha, isAutoRefresh = false) {
    try {
        const res = await request('/usuarios/login', {
            method: 'POST',
            body: JSON.stringify({ usuario, senha })
        });
        
        state.credentials = { usuario, senha };
        localStorage.setItem('credentials', JSON.stringify(state.credentials));
        state.token = res.dados.token; // Definir o token imediatamente para a próxima requisição funcionar
        
        if (!isAutoRefresh) {
            // Fazer a requisição automática de GET /usuarios/{id} apenas após o login manual
            const profileRes = await request(`/usuarios/${res.dados.usuario.id}`);
            await setupSession(res.dados.token, profileRes.dados);
            showToast('Bem-vindo de volta!', 'success');
            renderPage('profile');
        } else {
            // Renovação silenciosa de token, sem buscar o perfil novamente
            await setupSession(res.dados.token, state.user);
        }
        return true;
    } catch (err) {
        if (isAutoRefresh) {
            console.error("Falha na renovação automática:", err);
            logout();
        }
        return false;
    }
}

async function setupSession(token, user = null) {
    state.token = token;
    localStorage.setItem('token', token);

    if (!user) {
        try {
            const payload = decodeJWT(token);
            if (payload && payload.sub) {
                const res = await request(`/usuarios/${payload.sub}`);
                user = res.dados;
            }
        } catch (err) {
            console.error("Erro ao recuperar perfil:", err);
            logout();
            return;
        }
    }

    state.user = user;
    localStorage.setItem('user', JSON.stringify(user));

    const payload = decodeJWT(token);
    if (payload && payload.exp && state.credentials) {
        const expirationTime = payload.exp * 1000;
        const now = Date.now();
        const ttl = expirationTime - now;

        if (state.refreshTimer) clearTimeout(state.refreshTimer);

        const refreshThreshold = 30000;
        if (ttl > refreshThreshold) {
            state.refreshTimer = setTimeout(() => {
                login(state.credentials.usuario, state.credentials.senha, true);
            }, ttl - refreshThreshold);
        } else {
             login(state.credentials.usuario, state.credentials.senha, true);
        }
    }
}

function logout() {
    request('/usuarios/logout', { method: 'POST' })
        .catch(() => {});
    
    state.token = null;
    state.user = null;
    state.credentials = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('credentials');
    if (state.refreshTimer) clearTimeout(state.refreshTimer);
    
    renderPage('login');
}

// --- Auxiliares ---
function parseApiUrl(url) {
    if (!url) return { ip: '127.0.0.1', port: '8080' };
    let cleaned = url.replace(/^https?:\/\//i, '');
    let parts = cleaned.split(':');
    let ip = parts[0] || '127.0.0.1';
    let port = parts[1] || '23900';
    return { ip, port };
}

// --- Renderização de Páginas ---

const pages = {
    login: () => {
        const { ip, port } = parseApiUrl(state.apiUrl);
        return `
        <div class="main-container">
            <h1 class="logo">Instagram</h1>
            <form id="login-form">
                <div class="form-group"><input type="text" name="username" placeholder="Usuário" required></div>
                <div class="form-group"><input type="password" name="password" placeholder="Senha" required></div>
                <button type="submit">Entrar</button>
            </form>
            <div class="switch-page">
                Não tem uma conta? <a onclick="renderPage('register')">Cadastre-se</a>
            </div>
            
            <div class="api-config">
                <button class="link-btn" id="btn-toggle-api">⚙ Configurar Servidor</button>
                <div id="api-settings" style="display: none; margin-top: 10px;">
                    <div style="display: flex; gap: 8px; align-items: flex-end;">
                        <div class="form-group" style="flex: 2; margin-bottom: 0;">
                            <label style="font-size: 11px; margin-bottom: 2px;">IP do Servidor:</label>
                            <input type="text" id="api-ip-input" value="${ip}" placeholder="127.0.0.1">
                        </div>
                        <div class="form-group" style="flex: 1; margin-bottom: 0;">
                            <label style="font-size: 11px; margin-bottom: 2px;">Porta:</label>
                            <input type="text" id="api-port-input" value="${port}" placeholder="8080">
                        </div>
                    </div>
                    <button class="secondary small" onclick="saveApiConfig()" style="margin-top: 10px; width: 100%;">Salvar e Recarregar</button>
                </div>
            </div>
        </div>
    `;
    },
    register: () => `
        <div class="main-container">
            <h1 class="logo">Instagram</h1>
            <p style="color: var(--insta-gray); margin-bottom: 20px;">Cadastre-se para ver fotos e vídeos dos seus amigos.</p>
            <form id="register-form">
                <div class="form-group"><input type="text" name="reg-nome" placeholder="Nome Completo" required></div>
                <div class="form-group"><input type="email" name="reg-email" placeholder="E-mail" required></div>
                <div class="form-group"><input type="text" name="reg-user" placeholder="Nome de usuário" required></div>
                <div class="form-group"><input type="password" name="reg-pass" placeholder="Senha" required></div>
                <button type="submit">Cadastrar</button>
            </form>
            <div class="switch-page">
                Tem uma conta? <a onclick="renderPage('login')">Conecte-se</a>
            </div>
        </div>
    `,
    profile: () => `
        <div class="main-container" style="max-width: 600px;">
            <h1 class="logo">Instagram</h1>
            <div id="profile-display">
                <img src="${(state.user && state.user.foto) || 'https://via.placeholder.com/150'}" class="profile-pic" id="disp-foto">
                <div class="profile-info">
                    <h2 class="profile-username" id="disp-user">${(state.user && (state.user.usuario || state.user.nome)) || ''}</h2>
                    <p class="profile-bio" id="disp-bio">${(state.user && state.user.biografia) || 'Sem biografia.'}</p>
                </div>
                <div style="margin-top: 30px;">
                    <button onclick="renderPage('edit')">Editar Perfil</button>
                    <button onclick="renderPage('admin')" style="margin-top: 10px;">Painel de Usuários (Teste Admin)</button>
                    <button class="secondary" onclick="logout()" style="margin-top: 10px;">Sair</button>
                    <button class="danger" onclick="deleteAccount()" style="margin-top: 20px;">Excluir Conta</button>
                </div>
                <div style="margin-top: 20px; font-size: 10px; color: var(--insta-gray);">
                    Conectado a: ${state.apiUrl || 'Servidor Local'}
                </div>
            </div>
        </div>
    `,
    admin: () => `
        <div class="main-container" style="max-width: 800px; width: 100%;">
            <h1 class="logo">Painel de Usuários</h1>
            
            <div style="display: flex; gap: 15px; margin-bottom: 20px; justify-content: center; align-items: center; flex-wrap: wrap;">
                <div style="display: flex; gap: 5px; align-items: center;">
                    <input type="text" id="manual-edit-id-input" placeholder="ID Usuário" style="width: 100px; padding: 6px; margin: 0;">
                    <button class="secondary small" onclick="triggerManualEdit()" style="margin: 0; padding: 6px 12px; font-size: 0.8rem; width: auto;">Editar por ID</button>
                </div>
                <div style="display: flex; gap: 5px; align-items: center;">
                    <input type="text" id="manual-delete-id-input" placeholder="ID Usuário" style="width: 100px; padding: 6px; margin: 0;">
                    <button class="danger small" onclick="triggerManualDelete()" style="margin: 0; padding: 6px 12px; font-size: 0.8rem; width: auto; background-color: var(--insta-red);">Excluir por ID</button>
                </div>
            </div>

            <div id="admin-users-list">
                <p>Carregando usuários...</p>
            </div>
            
            <div id="admin-edit-container" style="display: none; margin-top: 30px; border-top: 1px solid var(--insta-border); padding-top: 20px; text-align: left;">
                <h3 style="margin-bottom: 15px;">Editar Usuário (<span id="admin-edit-username-title"></span>)</h3>
                <form id="admin-user-edit-form">
                    <input type="hidden" id="admin-edit-id">
                    <div class="form-group"><label>Nome</label><input type="text" id="admin-edit-nome"></div>
                    <div class="form-group"><label>Usuário</label><input type="text" id="admin-edit-user"></div>
                    <div class="form-group"><label>E-mail</label><input type="email" id="admin-edit-email"></div>
                    <div class="form-group"><label>Biografia</label><textarea id="admin-edit-bio" rows="2"></textarea></div>
                    <div class="form-group"><label>URL da Foto</label><input type="text" id="admin-edit-foto"></div>
                    <div class="form-group"><label>Senha (opcional)</label><input type="password" id="admin-edit-pass" placeholder="Manter atual"></div>
                    
                    <div style="display: flex; gap: 8px;">
                        <button type="submit" style="margin-top: 10px; flex: 1;">Salvar Alterações</button>
                        <button type="button" class="secondary" id="btn-cancel-admin-edit" style="margin-top: 10px; flex: 1;">Cancelar</button>
                    </div>
                </form>
            </div>

            <div style="margin-top: 30px;">
                <button class="secondary" onclick="renderPage('profile')">Voltar ao Perfil</button>
            </div>
        </div>
    `,
    edit: () => `
        <div class="main-container" style="max-width: 450px;">
            <h1 class="logo">Editar Perfil</h1>
            <form id="edit-form">
                <div class="form-group"><label>Nome</label><input type="text" name="edit-nome" placeholder="${(state.user && state.user.nome) || ''}"></div>
                <div class="form-group"><label>Usuário</label><input type="text" name="edit-user" placeholder="${(state.user && state.user.usuario) || ''}"></div>
                <div class="form-group"><label>E-mail</label><input type="email" name="edit-email" placeholder="${(state.user && state.user.email) || ''}"></div>
                <div class="form-group"><label>Biografia</label><textarea name="edit-bio" rows="3" placeholder="${(state.user && state.user.biografia) || ''}"></textarea></div>
                <div class="form-group"><label>URL da Foto</label><input type="text" name="edit-foto" placeholder="${(state.user && state.user.foto) || ''}"></div>
                <div class="form-group"><label>Nova Senha (opcional)</label><input type="password" name="edit-pass" placeholder="Deixe em branco para manter"></div>
                <button type="submit">Salvar Alterações</button>
                <button type="button" class="secondary" onclick="renderPage('profile')">Cancelar</button>
            </form>
        </div>
    `
};

function renderPage(pageName) {
    if ((pageName === 'profile' || pageName === 'edit' || pageName === 'admin') && !state.token) {
        pageName = 'login';
    }
    if ((pageName === 'login' || pageName === 'register') && state.token) {
        pageName = 'profile';
    }

    const app = document.getElementById('app');
    const loader = document.getElementById('loader');
    
    if (loader) loader.style.display = 'flex';
    state.currentPage = pageName;
    
    setTimeout(() => {
        if (app) app.innerHTML = pages[pageName]();
        attachEvents(pageName);
        if (loader) loader.style.display = 'none';
    }, 300);
}

// --- Eventos ---

function saveApiConfig() {
    const ipInput = document.getElementById('api-ip-input');
    const portInput = document.getElementById('api-port-input');
    if (ipInput && portInput) {
        let ip = ipInput.value.trim();
        let port = portInput.value.trim();
        
        if (ip.endsWith('.')) {
            ip = ip.slice(0, -1);
        }
        
        if (!ip) ip = '127.0.0.1';
        if (!port) port = '8080';
        
        const newUrl = `http://${ip}:${port}`;
        
        localStorage.setItem('api_url', newUrl);
        state.apiUrl = newUrl;
        showToast("Configuração salva! Recarregando...", "success");
        setTimeout(() => location.reload(), 1000);
    }
}

function attachEvents(pageName) {
    if (pageName === 'login') {
        const form = document.getElementById('login-form');
        if (form) form.onsubmit = async (e) => {
            e.preventDefault();
            const usuario = e.target.username.value;
            const senha = e.target.password.value;
            await login(usuario, senha);
        };

        const btnToggle = document.getElementById('btn-toggle-api');
        const apiSettings = document.getElementById('api-settings');
        if (btnToggle && apiSettings) {
            btnToggle.onclick = () => {
                const isHidden = apiSettings.style.display === 'none';
                apiSettings.style.display = isHidden ? 'block' : 'none';
            };
        }

        const ipInput = document.getElementById('api-ip-input');
        const portInput = document.getElementById('api-port-input');

        if (ipInput) {
            ipInput.addEventListener('input', (e) => {
                let val = e.target.value.replace(/[^0-9.]/g, '');
                
                // limit dots to 3
                const parts = val.split('.');
                if (parts.length > 4) {
                    val = parts.slice(0, 4).join('.');
                }
                
                // limit each part to 3 digits
                const cleanedParts = val.split('.').map(part => part.slice(0, 3));
                val = cleanedParts.join('.');
                
                e.target.value = val;
            });
        }

        if (portInput) {
            portInput.addEventListener('input', (e) => {
                e.target.value = e.target.value.replace(/[^0-9]/g, '').slice(0, 5);
            });
        }
    } else if (pageName === 'register') {
        const form = document.getElementById('register-form');
        if (form) form.onsubmit = async (e) => {
            e.preventDefault();
            const body = {
                nome: e.target['reg-nome'].value,
                email: e.target['reg-email'].value,
                usuario: e.target['reg-user'].value,
                senha: e.target['reg-pass'].value
            };
            try {
                await request('/usuarios', {
                    method: 'POST',
                    body: JSON.stringify(body)
                });
                showToast('Cadastro realizado! Faça login.', 'success');
                renderPage('login');
            } catch (err) {}
        };
    } else if (pageName === 'edit') {
        const form = document.getElementById('edit-form');
        if (form) form.onsubmit = async (e) => {
            e.preventDefault();
            const body = {};
            
            const fields = {
                nome: e.target['edit-nome'].value,
                usuario: e.target['edit-user'].value,
                email: e.target['edit-email'].value,
                biografia: e.target['edit-bio'].value,
                foto: e.target['edit-foto'].value
            };

            // Adicionar ao body apenas o que mudou e não está vazio
            Object.keys(fields).forEach(key => {
                const value = fields[key].trim();
                if (value && value !== state.user[key]) {
                    body[key] = value;
                }
            });

            const senha = e.target['edit-pass'].value;
            if (senha) body.senha = senha;

            if (Object.keys(body).length === 0) {
                showToast('Nenhuma alteração detectada', 'info');
                return;
            }

            try {
                const res = await request(`/usuarios/${state.user.id}`, {
                    method: 'PATCH',
                    body: JSON.stringify(body)
                });
                
                if (!state.credentials && state.user) {
                    state.credentials = { usuario: state.user.usuario, senha: '' };
                }

                const usernameChanged = body.usuario && body.usuario !== state.credentials?.usuario;
                if (state.credentials) {
                    if (body.usuario) state.credentials.usuario = body.usuario;
                    if (body.senha) state.credentials.senha = body.senha;
                    localStorage.setItem('credentials', JSON.stringify(state.credentials));
                }

                state.user = res.dados;
                localStorage.setItem('user', JSON.stringify(state.user));

                if (usernameChanged && state.credentials) {
                    await login(state.credentials.usuario, state.credentials.senha, true);
                }

                showToast('Perfil atualizado!', 'success');
                renderPage('profile');
            } catch (err) {}
        };
    } else if (pageName === 'admin') {
        loadAdminUsers();

        const editForm = document.getElementById('admin-user-edit-form');
        if (editForm) {
            editForm.onsubmit = async (e) => {
                e.preventDefault();
                const userId = document.getElementById('admin-edit-id').value;
                
                const body = {};
                const fields = {
                    nome: document.getElementById('admin-edit-nome').value.trim(),
                    usuario: document.getElementById('admin-edit-user').value.trim(),
                    email: document.getElementById('admin-edit-email').value.trim(),
                    biografia: document.getElementById('admin-edit-bio').value.trim(),
                    foto: document.getElementById('admin-edit-foto').value.trim()
                };

                // Same comparison logic as profile edit screen
                Object.keys(fields).forEach(key => {
                    const value = fields[key];
                    const originalValue = (state.adminEditingUser && state.adminEditingUser[key]) || '';
                    if (value && value !== originalValue) {
                        body[key] = value;
                    }
                });

                const senha = document.getElementById('admin-edit-pass').value;
                if (senha) body.senha = senha;

                if (Object.keys(body).length === 0) {
                    showToast('Nenhuma alteração detectada', 'info');
                    return;
                }

                try {
                    await request(`/usuarios/${userId}`, {
                        method: 'PATCH',
                        body: JSON.stringify(body)
                    });
                    
                    showToast('Usuário atualizado com sucesso!', 'success');
                    
                    if (userId === String(state.user?.id || '')) {
                        const updatedProfile = await request(`/usuarios/${userId}`);
                        state.user = updatedProfile.dados;
                        localStorage.setItem('user', JSON.stringify(state.user));
                        
                        if (state.credentials) {
                            if (body.usuario) state.credentials.usuario = body.usuario;
                            if (senha) state.credentials.senha = senha;
                            localStorage.setItem('credentials', JSON.stringify(state.credentials));
                        }
                    }

                    document.getElementById('admin-edit-container').style.display = 'none';
                    loadAdminUsers();
                } catch (err) {
                    console.error("Erro ao atualizar usuário:", err);
                }
            };
        }

        const btnCancelEdit = document.getElementById('btn-cancel-admin-edit');
        if (btnCancelEdit) {
            btnCancelEdit.onclick = () => {
                document.getElementById('admin-edit-container').style.display = 'none';
            };
        }

        // Live input filters for digit-only IDs
        const manualEditId = document.getElementById('manual-edit-id-input');
        const manualDeleteId = document.getElementById('manual-delete-id-input');
        if (manualEditId) {
            manualEditId.addEventListener('input', (e) => {
                e.target.value = e.target.value.replace(/[^0-9]/g, '');
            });
        }
        if (manualDeleteId) {
            manualDeleteId.addEventListener('input', (e) => {
                e.target.value = e.target.value.replace(/[^0-9]/g, '');
            });
        }
    }
}

async function deleteAccount() {
    if (confirm('Tem certeza que deseja excluir sua conta? Esta ação é irreversível.')) {
        try {
            await request(`/usuarios/${state.user.id}`, { method: 'DELETE' });
            showToast('Conta excluída com sucesso.', 'success');
            logout();
        } catch (err) {}
    }
}

// --- Inicialização ---

let apiSpec = null;

async function initRequestBuilder() {
    const pathsToTry = [
        '../instagram-api.json',
        'instagram-api.json',
        './instagram-api.json',
        '/instagram-api.json',
        '/instagram_client/instagram-api.json'
    ];

    let success = false;
    let lastError = null;

    for (const path of pathsToTry) {
        try {
            const response = await fetch(path);
            if (response.ok) {
                apiSpec = await response.json();
                console.log(`Especificação da API carregada com sucesso do caminho: ${path}`);
                success = true;
                break;
            } else {
                throw new Error(`Status HTTP ${response.status}`);
            }
        } catch (err) {
            lastError = err;
        }
    }

    if (!success) {
        console.error("Erro ao carregar instagram-api.json de todos os caminhos possíveis:", lastError);
        showToast("Erro ao carregar os modelos da API. Use um servidor local (HTTP) para evitar bloqueios do navegador.", "error");
        return;
    }

    try {
        const select = document.getElementById('request-template-select');
        if (!select) return;

        // Limpa opções antigas exceto a primeira ("Selecione um endpoint...")
        while (select.options.length > 1) {
            select.remove(1);
        }

        Object.keys(apiSpec.paths).forEach(path => {
            const methods = apiSpec.paths[path];
            Object.keys(methods).forEach(method => {
                const op = methods[method];
                const option = document.createElement('option');
                option.value = `${method.toUpperCase()}|${path}`;
                option.innerText = `${method.toUpperCase()} ${path} (${op.summary || ''})`;
                select.appendChild(option);
            });
        });

        select.onchange = (e) => {
            const [method, path] = e.target.value.split('|');
            if (!method) return;

            document.getElementById('builder-method').value = method;
            
            // Auto-preencher URL (substituindo {id} se possível)
            let finalPath = path;
            if (finalPath.includes('{id}') && state.user && state.user.id) {
                finalPath = finalPath.replace('{id}', state.user.id);
            }
            document.getElementById('builder-url').value = finalPath;

            // Auto-preencher Token
            if (state.token) {
                document.getElementById('builder-token').value = `Bearer ${state.token}`;
            } else {
                document.getElementById('builder-token').value = '';
            }

            // Auto-preencher Corpo (se houver exemplo)
            const opData = apiSpec.paths[path][method.toLowerCase()];
            if (opData && opData.requestBody && opData.requestBody.content['application/json']) {
                const example = opData.requestBody.content['application/json'].example;
                document.getElementById('builder-body').value = JSON.stringify(example, null, 2);
            } else {
                document.getElementById('builder-body').value = '';
            }
        };

        document.getElementById('btn-send-custom').onclick = async () => {
            const method = document.getElementById('builder-method').value;
            const path = document.getElementById('builder-url').value;
            const token = document.getElementById('builder-token').value;
            const bodyStr = document.getElementById('builder-body').value;

            const headers = { 'Content-Type': 'application/json' };
            if (token) headers['Authorization'] = token;

            let body = null;
            if (bodyStr && bodyStr.trim() !== '') {
                try {
                    body = JSON.parse(bodyStr);
                } catch (e) {
                    showToast('JSON do corpo inválido', 'error');
                    return;
                }
            }

            try {
                // Usamos a função request padrão mas com overrides se necessário
                // Ou fazemos um fetch direto para total liberdade (como solicitado "qualquer parâmetro")
                const baseUrl = state.apiUrl.endsWith('/') ? state.apiUrl.slice(0, -1) : state.apiUrl;
                const url = `${baseUrl}${path.startsWith('/') ? path : '/' + path}`;

                logDebug('info', `CUSTOM REQUEST: ${method} ${path}`, { headers, body });

                const res = await fetch(url, {
                    method: method,
                    headers: headers,
                    body: body ? JSON.stringify(body) : null
                });

                const data = await res.json();
                logDebug(res.ok ? 'res' : 'err', `CUSTOM RESPONSE ${path}`, data);
                if (res.ok) showToast('Requisição customizada enviada!', 'success');
            } catch (err) {
                logDebug('err', `CUSTOM ERROR ${path}`, err);
                showToast('Erro na requisição customizada', 'error');
            }
        };
    } catch (err) {
        console.error("Erro ao inicializar o Request Builder:", err);
    }
}

// --- Funções Administrativas ---

async function loadAdminUsers() {
    const listContainer = document.getElementById('admin-users-list');
    if (!listContainer) return;

    try {
        const res = await request('/usuarios');
        const users = (res.dados && res.dados.usuarios) || [];

        if (users.length === 0) {
            listContainer.innerHTML = `<p style="color: var(--insta-gray); text-align: center; margin-top: 15px;">Nenhum usuário cadastrado.</p>`;
            return;
        }

        let html = `
            <div style="overflow-x: auto; margin-top: 20px;">
                <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.85rem;">
                    <thead>
                        <tr style="border-bottom: 2px solid var(--insta-border); color: var(--insta-gray); font-weight: bold;">
                            <th style="padding: 10px 5px;">ID</th>
                            <th style="padding: 10px 5px;">Foto</th>
                            <th style="padding: 10px 5px;">Usuário</th>
                            <th style="padding: 10px 5px;">Nome</th>
                            <th style="padding: 10px 5px;">E-mail</th>
                            <th style="padding: 10px 5px; text-align: center;">Ações</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        users.forEach(u => {
            const userPic = u.foto || 'https://via.placeholder.com/150';
            html += `
                <tr style="border-bottom: 1px solid var(--insta-border); transition: background 0.2s;" class="admin-user-row">
                    <td style="padding: 10px 5px; font-weight: bold; color: var(--insta-gray);">${u.id}</td>
                    <td style="padding: 10px 5px;">
                        <img src="${userPic}" style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover; border: 1px solid var(--insta-border);">
                    </td>
                    <td style="padding: 10px 5px; font-weight: bold; color: var(--insta-black);">${u.usuario}</td>
                    <td style="padding: 10px 5px;">${u.nome}</td>
                    <td style="padding: 10px 5px; color: var(--insta-gray);">${u.email}</td>
                    <td style="padding: 10px 5px; text-align: center; white-space: nowrap;">
                        <button class="secondary small" onclick="openAdminEdit('${u.id}')" style="margin: 0 4px; padding: 6px 10px; font-size: 0.75rem; width: auto; display: inline-block;" title="Editar"><span id="edit-arrow-icon-${u.id}" style="font-size: 0.8rem; vertical-align: middle;">▼</span></button>
                        <button class="danger small" onclick="deleteUserByAdmin('${u.id}')" style="margin: 0 4px; padding: 6px 10px; font-size: 0.75rem; width: auto; display: inline-block; background-color: var(--insta-red);" title="Excluir"><svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: white; vertical-align: middle;"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg></button>
                    </td>
                </tr>
                <tr id="edit-row-${u.id}" class="admin-edit-row-container" style="display: none; background-color: #fafafa;">
                    <td colspan="6" style="padding: 0;">
                        <div id="edit-content-${u.id}" class="admin-edit-content-wrapper"></div>
                    </td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        listContainer.innerHTML = html;
    } catch (err) {
        listContainer.innerHTML = `<p style="color: var(--insta-red); text-align: center; margin-top: 15px;">Erro ao carregar usuários do servidor.</p>`;
    }
}

const closeTimeouts = {};

window.openAdminEdit = async (id) => {
    try {
        const res = await request(`/usuarios/${id}`);
        const user = res.dados;
        if (!user) {
            showToast("Usuário não encontrado", "error");
            return;
        }

        state.adminEditingUser = user;

        const inlineContainer = document.getElementById(`edit-content-${id}`);
        const inlineRow = document.getElementById(`edit-row-${id}`);
        const arrowIcon = document.getElementById(`edit-arrow-icon-${id}`);

        const formHtml = `
            <form id="admin-user-edit-form-${user.id}" style="padding: 15px; background: var(--insta-bg); border: 1px solid var(--insta-border); border-radius: 4px; text-align: left; display: flex; flex-direction: column; gap: 8px; margin: 10px 0;">
                <h4 style="margin-bottom: 8px;">Editar Usuário (${user.usuario})</h4>
                <div class="form-group"><label style="font-size: 11px;">Nome</label><input type="text" id="admin-edit-nome-${user.id}" placeholder="${user.nome || ''}"></div>
                <div class="form-group"><label style="font-size: 11px;">Usuário</label><input type="text" id="admin-edit-user-${user.id}" placeholder="${user.usuario || ''}"></div>
                <div class="form-group"><label style="font-size: 11px;">E-mail</label><input type="email" id="admin-edit-email-${user.id}" placeholder="${user.email || ''}"></div>
                <div class="form-group"><label style="font-size: 11px;">Biografia</label><textarea id="admin-edit-bio-${user.id}" rows="2" placeholder="${user.biografia || 'Sem biografia.'}"></textarea></div>
                <div class="form-group"><label style="font-size: 11px;">URL da Foto</label><input type="text" id="admin-edit-foto-${user.id}" placeholder="${user.foto || ''}"></div>
                <div class="form-group"><label style="font-size: 11px;">Senha (opcional)</label><input type="password" id="admin-edit-pass-${user.id}" placeholder="Manter atual"></div>
                
                <div style="display: flex; gap: 8px; margin-top: 10px;">
                    <button type="submit" style="margin: 0; padding: 8px; flex: 1;">Salvar</button>
                    <button type="button" class="secondary" onclick="closeAdminInlineEdit('${user.id}')" style="margin: 0; padding: 8px; flex: 1;">Cancelar</button>
                </div>
            </form>
        `;

        if (inlineContainer && inlineRow) {
            // Check if already open
            if (inlineContainer.classList.contains('open')) {
                // Collapse and exit
                closeAdminInlineEdit(id);
                return;
            }

            // Clear any pending close timeout for this ID to allow clean reuse
            if (closeTimeouts[id]) {
                clearTimeout(closeTimeouts[id]);
                delete closeTimeouts[id];
            }

            // Close any other open edit row first for smooth UX
            document.querySelectorAll('.admin-edit-content-wrapper.open').forEach(content => {
                const otherId = content.id.replace('edit-content-', '');
                if (otherId !== id) {
                    closeAdminInlineEdit(otherId);
                }
            });

            // Reset all other arrow icons to down arrow
            document.querySelectorAll('[id^="edit-arrow-icon-"]').forEach(span => {
                span.innerText = '▼';
            });

            inlineContainer.innerHTML = formHtml;
            inlineRow.style.display = 'table-row';
            
            // Set initial state for dynamic smooth animation
            inlineContainer.style.maxHeight = '0px';
            inlineContainer.style.padding = '0 15px';
            inlineContainer.style.opacity = '0';
            
            // Force reflow
            inlineContainer.offsetHeight;
            
            const targetHeight = inlineContainer.scrollHeight;
            
            // Trigger transition animation
            setTimeout(() => {
                inlineContainer.style.maxHeight = targetHeight + 'px';
                inlineContainer.style.padding = '15px';
                inlineContainer.style.opacity = '1';
                inlineContainer.classList.add('open');
                if (arrowIcon) arrowIcon.innerText = '▲'; // Set to up arrow
                inlineRow.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }, 30);
            
            bindAdminEditSubmit(user.id);
        } else {
            // Fallback to static panel at bottom (e.g. when list users request didn't work)
            const staticContainer = document.getElementById('admin-edit-container');
            if (staticContainer) {
                document.getElementById('admin-edit-username-title').innerText = user.usuario;
                document.getElementById('admin-edit-id').value = user.id;
                
                document.getElementById('admin-edit-nome').value = '';
                document.getElementById('admin-edit-user').value = '';
                document.getElementById('admin-edit-email').value = '';
                document.getElementById('admin-edit-bio').value = '';
                document.getElementById('admin-edit-foto').value = '';
                document.getElementById('admin-edit-pass').value = '';

                document.getElementById('admin-edit-nome').placeholder = user.nome || '';
                document.getElementById('admin-edit-user').placeholder = user.usuario || '';
                document.getElementById('admin-edit-email').placeholder = user.email || '';
                document.getElementById('admin-edit-bio').placeholder = user.biografia || 'Sem biografia.';
                document.getElementById('admin-edit-foto').placeholder = user.foto || '';

                staticContainer.style.display = 'block';
                staticContainer.scrollIntoView({ behavior: 'smooth' });
            }
        }
    } catch (err) {
        console.error("Erro ao carregar dados do usuário para edição:", err);
    }
};

window.closeAdminInlineEdit = (id) => {
    const inlineContainer = document.getElementById(`edit-content-${id}`);
    const inlineRow = document.getElementById(`edit-row-${id}`);
    const arrowIcon = document.getElementById(`edit-arrow-icon-${id}`);
    
    if (inlineContainer && inlineRow) {
        // Set dynamic current height before retracting to avoid jumpiness
        inlineContainer.style.maxHeight = inlineContainer.scrollHeight + 'px';
        inlineContainer.offsetHeight; // reflow
        
        inlineContainer.style.maxHeight = '0px';
        inlineContainer.style.padding = '0 15px';
        inlineContainer.style.opacity = '0';
        inlineContainer.classList.remove('open');
        
        if (arrowIcon) arrowIcon.innerText = '▼'; // Set back to down arrow
        
        if (closeTimeouts[id]) {
            clearTimeout(closeTimeouts[id]);
        }
        
        closeTimeouts[id] = setTimeout(() => {
            inlineRow.style.display = 'none';
            inlineContainer.innerHTML = '';
            delete closeTimeouts[id];
        }, 400); // match transition duration
    } else {
        const staticContainer = document.getElementById('admin-edit-container');
        if (staticContainer) {
            staticContainer.style.display = 'none';
        }
    }
};

function bindAdminEditSubmit(userId) {
    const form = document.getElementById(`admin-user-edit-form-${userId}`);
    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();
            
            const body = {};
            const fields = {
                nome: document.getElementById(`admin-edit-nome-${userId}`).value.trim(),
                usuario: document.getElementById(`admin-edit-user-${userId}`).value.trim(),
                email: document.getElementById(`admin-edit-email-${userId}`).value.trim(),
                biografia: document.getElementById(`admin-edit-bio-${userId}`).value.trim(),
                foto: document.getElementById(`admin-edit-foto-${userId}`).value.trim()
            };

            Object.keys(fields).forEach(key => {
                const value = fields[key];
                const originalValue = (state.adminEditingUser && state.adminEditingUser[key]) || '';
                if (value && value !== originalValue) {
                    body[key] = value;
                }
            });

            const senha = document.getElementById(`admin-edit-pass-${userId}`).value;
            if (senha) body.senha = senha;

            if (Object.keys(body).length === 0) {
                showToast('Nenhuma alteração detectada', 'info');
                return;
            }

            try {
                await request(`/usuarios/${userId}`, {
                    method: 'PATCH',
                    body: JSON.stringify(body)
                });
                
                showToast('Usuário atualizado com sucesso!', 'success');
                
                if (userId === String(state.user?.id || '')) {
                    const updatedProfile = await request(`/usuarios/${userId}`);
                    state.user = updatedProfile.dados;
                    localStorage.setItem('user', JSON.stringify(state.user));
                    
                    if (state.credentials) {
                        if (body.usuario) state.credentials.usuario = body.usuario;
                        if (senha) state.credentials.senha = senha;
                        localStorage.setItem('credentials', JSON.stringify(state.credentials));
                    }
                }

                closeAdminInlineEdit(userId);
                loadAdminUsers();
            } catch (err) {
                console.error("Erro ao atualizar usuário:", err);
            }
        };
    }
}

window.deleteUserByAdmin = async (id, usuario) => {
    try {
        let finalUsuario = usuario;
        if (!finalUsuario) {
            const res = await request(`/usuarios/${id}`);
            finalUsuario = res.dados?.usuario || res.dados?.nome || `ID: ${id}`;
        }

        if (confirm(`Tem certeza de que deseja remover permanentemente o usuário "${finalUsuario}"?`)) {
            await request(`/usuarios/${id}`, {
                method: 'DELETE'
            });
            showToast(`Usuário "${finalUsuario}" removido com sucesso!`, 'success');
            
            if (id === String(state.user?.id || '')) {
                logout();
                return;
            }

            loadAdminUsers();
            
            closeAdminInlineEdit(id);
        }
    } catch (err) {
        console.error("Erro ao remover usuário:", err);
    }
};

window.triggerManualEdit = () => {
    const input = document.getElementById('manual-edit-id-input');
    if (input) {
        const id = input.value.trim();
        if (!id) {
            showToast("Digite um ID válido", "error");
            return;
        }
        openAdminEdit(id);
    }
};

window.triggerManualDelete = () => {
    const input = document.getElementById('manual-delete-id-input');
    if (input) {
        const id = input.value.trim();
        if (!id) {
            showToast("Digite um ID válido", "error");
            return;
        }
        deleteUserByAdmin(id);
    }
};

document.addEventListener('DOMContentLoaded', async () => {
    const toggleBtn = document.getElementById('toggle-debug-area');
    if (toggleBtn) {
        toggleBtn.onclick = () => {
            const debugArea = document.getElementById('debug-area');
            if (debugArea) {
                debugArea.classList.toggle('collapsed');
                toggleBtn.innerText = debugArea.classList.contains('collapsed') ? '▲' : '▼';
            }
        };
    }

    initRequestBuilder();

    if (state.token) {
        await setupSession(state.token, state.user);
        renderPage('profile');
    } else {
        renderPage('login');
    }
});
