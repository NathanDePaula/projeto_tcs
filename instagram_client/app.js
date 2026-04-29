// Configuração Padrão (Vazio = mesmo host do frontend)
const DEFAULT_API_URL = '';

// Gerenciamento de Estado
const state = {
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    apiUrl: localStorage.getItem('api_url') || DEFAULT_API_URL,
    currentPage: 'login',
    refreshTimer: null,
    credentials: null 
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
    
    const defaultHeaders = {
        'Content-Type': 'application/json',
    };

    if (state.token) {
        defaultHeaders['Authorization'] = `Bearer ${state.token}`;
    }

    const fetchOptions = Object.assign({}, options, {
        headers: Object.assign({}, defaultHeaders, options.headers)
    });

    const requestLog = {
        url: url,
        method: options.method || 'GET',
        headers: fetchOptions.headers,
        body: fetchOptions.body ? JSON.parse(fetchOptions.body) : null
    };

    console.log("REQUISIÇÃO ENVIADA:", requestLog);
    logDebug('req', `FETCH ${options.method || 'GET'} ${endpoint}`, requestLog.body || {});

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
            throw data;
        }

        return data;
    } catch (error) {
        console.error("ERRO NA REQUISIÇÃO:", error);
        if (error.codigo === "TOKEN_EXPIRADO" || error.status === 403) {
            console.log("Sessão expirada, tentando renovação automática...");
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
        state.token = res.dados.token; // Definir o token imediatamente para a próxima requisição funcionar
        
        // Fazer a requisição automática de GET /usuarios/{id} após o login
        const profileRes = await request(`/usuarios/${res.dados.usuario.id}`);
        await setupSession(res.dados.token, profileRes.dados);
        
        if (!isAutoRefresh) {
            showToast('Bem-vindo de volta!', 'success');
            renderPage('profile');
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
    if (state.refreshTimer) clearTimeout(state.refreshTimer);
    
    renderPage('login');
}

// --- Renderização de Páginas ---

const pages = {
    login: () => `
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
                <button class="link-btn" id="btn-toggle-api">⚙ Configurar API</button>
                <div id="api-settings" style="display: none; margin-top: 10px;">
                    <div class="form-group">
                        <label style="font-size: 12px;">Endpoint da API:</label>
                        <input type="text" id="api-url-input" value="${state.apiUrl}" placeholder="http://localhost:8080">
                        <button class="secondary small" onclick="saveApiConfig()" style="margin-top: 5px;">Salvar e Recarregar</button>
                    </div>
                </div>
            </div>
        </div>
    `,
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
                    <button class="secondary" onclick="logout()">Sair</button>
                    <button class="danger" onclick="deleteAccount()" style="margin-top: 20px;">Excluir Conta</button>
                </div>
                <div style="margin-top: 20px; font-size: 10px; color: var(--insta-gray);">
                    Conectado a: ${state.apiUrl || 'Servidor Local'}
                </div>
            </div>
        </div>
    `,
    edit: () => `
        <div class="main-container" style="max-width: 450px;">
            <h1 class="logo">Editar Perfil</h1>
            <form id="edit-form">
                <div class="form-group"><label>Nome</label><input type="text" name="edit-nome" value="${(state.user && state.user.nome) || ''}"></div>
                <div class="form-group"><label>Usuário</label><input type="text" name="edit-user" value="${(state.user && state.user.usuario) || ''}"></div>
                <div class="form-group"><label>E-mail</label><input type="email" name="edit-email" value="${(state.user && state.user.email) || ''}"></div>
                <div class="form-group"><label>Biografia</label><textarea name="edit-bio" rows="3">${(state.user && state.user.biografia) || ''}</textarea></div>
                <div class="form-group"><label>URL da Foto</label><input type="text" name="edit-foto" value="${(state.user && state.user.foto) || ''}"></div>
                <div class="form-group"><label>Nova Senha (opcional)</label><input type="password" name="edit-pass" placeholder="Deixe em branco para manter"></div>
                <button type="submit">Salvar Alterações</button>
                <button type="button" class="secondary" onclick="renderPage('profile')">Cancelar</button>
            </form>
        </div>
    `
};

function renderPage(pageName) {
    if ((pageName === 'profile' || pageName === 'edit') && !state.token) {
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
    const input = document.getElementById('api-url-input');
    if (input) {
        const newUrl = input.value.trim();
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
                state.user = res.dados;
                localStorage.setItem('user', JSON.stringify(state.user));
                showToast('Perfil atualizado!', 'success');
                renderPage('profile');
            } catch (err) {}
        };
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

document.addEventListener('DOMContentLoaded', async () => {
    const toggleBtn = document.getElementById('toggle-console');
    if (toggleBtn) {
        toggleBtn.onclick = () => {
            const consoleEl = document.getElementById('debug-console');
            if (consoleEl) {
                consoleEl.classList.toggle('collapsed');
                toggleBtn.innerText = consoleEl.classList.contains('collapsed') ? '▲' : '▼';
            }
        };
    }

    if (state.token) {
        await setupSession(state.token, state.user);
        renderPage('profile');
    } else {
        renderPage('login');
    }
});
