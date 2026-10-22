<template>
  <main class="login-page">
    <div class="ambient ambient-one" aria-hidden="true"></div><div class="ambient ambient-two" aria-hidden="true"></div>
    <header class="login-header">
      <a href="/login" class="brand" aria-label="ShortLink 首页"><span class="brand-symbol"><el-icon><Link /></el-icon></span><span>ShortLink<span class="brand-dot">.</span></span></a>
      <span class="header-note">让每一次连接，都有回响</span>
    </header>
    <div class="login-layout">
      <section class="intro">
        <div class="eyebrow"><span></span> SMALL LINKS. BIG POSSIBILITIES.</div>
        <h1>缩短距离，<br />让连接<span class="iridescent">发光。</span></h1>
        <p class="intro-description">把冗长的网址，变成值得分享的连接。<br />轻松创建、管理，洞察每一次点击。</p>
        <div class="link-art" aria-hidden="true">
          <div class="orbit orbit-one"></div><div class="orbit orbit-two"></div><div class="glass-loop loop-one"></div><div class="glass-loop loop-two"></div>
          <div class="preview-link"><span class="preview-dot"></span><span>short.link / <b>possibility</b></span><el-icon><TopRight /></el-icon></div>
          <span class="art-caption">LESS LENGTH. MORE IMPACT.</span>
        </div>
        <div class="intro-features"><span><el-icon><Connection /></el-icon> 一个链接，无限可能</span><span><el-icon><DataLine /></el-icon> 每次点击，清晰可见</span></div>
      </section>
      <section class="auth-card" :aria-label="isLogin ? '账号登录' : '账号注册'">
        <div class="card-glint" aria-hidden="true"></div><div class="card-overline">YOUR CONNECTION STARTS HERE</div>
        <div class="auth-heading"><h2>{{ isLogin ? '欢迎回来' : '开启你的连接' }}<span class="heading-dot">.</span></h2><p>{{ isLogin ? '登录，继续创造下一次精彩连接。' : '创建账号，让你的分享更进一步。' }}</p></div>
        <div class="auth-tabs" role="tablist" aria-label="选择登录或注册">
          <button id="login-tab" type="button" role="tab" :aria-selected="isLogin" aria-controls="auth-panel" :class="{ active: isLogin }" @click="switchMode(true)">登录账号</button>
          <button id="register-tab" type="button" role="tab" :aria-selected="!isLogin" aria-controls="auth-panel" :class="{ active: !isLogin }" @click="switchMode(false)">注册账号</button>
        </div>
        <div id="auth-panel" role="tabpanel" :aria-labelledby="isLogin ? 'login-tab' : 'register-tab'">
          <el-form v-if="isLogin" ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @submit.prevent="login">
            <el-form-item label="用户名" prop="username"><el-input v-model="loginForm.username" placeholder="输入你的用户名" autocomplete="username" :prefix-icon="User" clearable /></el-form-item>
            <el-form-item label="密码" prop="password"><el-input v-model="loginForm.password" type="password" placeholder="输入你的密码" autocomplete="current-password" :prefix-icon="Lock" show-password /></el-form-item>
            <div class="form-options"><el-checkbox v-model="rememberUsername">记住用户名</el-checkbox><span>欢迎回到你的连接空间</span></div>
            <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">{{ loading ? '正在登录' : '进入我的空间' }}<el-icon v-if="!loading"><Right /></el-icon></el-button>
          </el-form>
          <el-form v-else ref="registerFormRef" :model="addForm" :rules="registerRules" label-position="top" @submit.prevent="register">
            <el-form-item label="用户名" prop="username"><el-input v-model="addForm.username" placeholder="设置用户名" autocomplete="username" :prefix-icon="User" /></el-form-item>
            <div class="form-row"><el-form-item label="姓名" prop="realName"><el-input v-model="addForm.realName" placeholder="你的姓名" autocomplete="name" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model="addForm.phone" placeholder="11 位手机号" autocomplete="tel" /></el-form-item></div>
            <el-form-item label="邮箱" prop="mail"><el-input v-model="addForm.mail" placeholder="you@example.com" autocomplete="email" /></el-form-item>
            <el-form-item label="密码" prop="password"><el-input v-model="addForm.password" type="password" placeholder="设置 8–15 位密码" autocomplete="new-password" :prefix-icon="Lock" show-password /></el-form-item>
            <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">创建账号<el-icon><Right /></el-icon></el-button>
          </el-form>
        </div>
        <div class="card-footer"><el-icon><Lock /></el-icon><span>专注分享，把连接交给 ShortLink</span></div>
      </section>
    </div>
    <footer class="login-footer"><span>© {{ new Date().getFullYear() }} ShortLink</span><span>每一个好想法，都值得被连接。</span><span class="footer-mark">DESIGNED TO CONNECT ↗</span></footer>
  </main>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { setToken, setUsername } from '@/core/auth.js'
const API = getCurrentInstance().proxy.$API
const router = useRouter()
const loginFormRef = ref()
const registerFormRef = ref()
const isLogin = ref(true)
const loading = ref(false)
const rememberUsername = ref(!!localStorage.getItem('rememberedUsername'))
const loginForm = reactive({ username: localStorage.getItem('rememberedUsername') || '', password: '' })
const addForm = reactive({ username: '', password: '', realName: '', phone: '', mail: '' })
const required = (message) => [{ required: true, message, trigger: 'blur' }]
const loginRules = { username: required('请输入用户名'), password: required('请输入密码') }
const registerRules = {
  username: required('请设置用户名'), realName: required('请输入姓名'),
  password: [...required('请设置密码'), { min: 8, max: 15, message: '密码长度为 8–15 位', trigger: 'blur' }],
  phone: [...required('请输入手机号'), { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }],
  mail: [...required('请输入邮箱'), { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }]
}
const switchMode = (value) => { if (!loading.value) isLogin.value = value }
const enterWorkspace = async (credentials) => {
  const { data } = await API.user.login(credentials)
  if (!data.success || !data.data?.token) throw new Error(data.message || '登录失败，请检查账号密码')
  setToken(data.data.token)
  setUsername(credentials.username)
  localStorage.setItem('token', data.data.token)
  localStorage.setItem('username', credentials.username)
  if (rememberUsername.value) localStorage.setItem('rememberedUsername', credentials.username)
  else localStorage.removeItem('rememberedUsername')
  ElMessage.success('登录成功，欢迎回来')
  await router.push('/home')
}
const reportError = (error) => ElMessage.error(error.response?.data?.message || error.message || '服务暂时不可用，请稍后重试')
const login = async () => {
  if (loading.value || !await loginFormRef.value?.validate().catch(() => false)) return
  loading.value = true
  try { await enterWorkspace(loginForm) } catch (error) { reportError(error) } finally { loading.value = false }
}
const register = async () => {
  if (loading.value || !await registerFormRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    const { data: availability } = await API.user.hasUsername({ username: addForm.username })
    if (!availability.success || availability.data !== true) throw new Error('用户名已存在，请换一个试试')
    const { data } = await API.user.addUser(addForm)
    if (!data.success) throw new Error(data.message || '注册失败，请稍后重试')
    loginForm.username = addForm.username
    loginForm.password = addForm.password
    isLogin.value = true
    ElMessage.success('注册成功')
    await enterWorkspace({ username: addForm.username, password: addForm.password })
  } catch (error) { reportError(error) } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.login-page { --ink: #f2f5ff; --muted: #a3adc4; position: relative; isolation: isolate; min-height: 100svh; overflow: hidden; padding: 35px 6vw 25px; color: var(--ink); background: #090e1d; font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.login-page::before { content: ''; position: absolute; inset: 0; z-index: -1; background: radial-gradient(ellipse at 48% 45%, #19244870, transparent 65%), linear-gradient(110deg, transparent 40%, #37345317); }
.ambient { position: absolute; width: 560px; height: 560px; border-radius: 50%; filter: blur(100px); pointer-events: none; z-index: -1; opacity: .18; }.ambient-one { background: #416bff; top: 6%; left: -220px; }.ambient-two { background: #8560ff; bottom: -300px; right: -140px; }
.login-header, .login-footer { display: flex; justify-content: space-between; align-items: center; max-width: 1320px; margin: auto; }
.brand { display: inline-flex; gap: 11px; align-items: center; color: #f3f7ff; text-decoration: none; font-weight: 650; font-size: 24px; letter-spacing: -.7px; }.brand-symbol { display: grid; place-items: center; width: 38px; height: 38px; border: 1px solid #91b8ff70; border-radius: 12px; background: linear-gradient(135deg, #7b99ff44, #82f6ed12); box-shadow: inset 0 1px 0 #ffffff60, 0 0 25px #557bff20; color: #b8d9ff; }.brand-dot { color: #8ce5e1; }.header-note { color: #a3adc4; font-size: 12px; letter-spacing: 2px; }
.login-layout { display: grid; grid-template-columns: 1fr 440px; align-items: center; gap: 90px; max-width: 1160px; min-height: calc(100svh - 170px); margin: 20px auto; }
.eyebrow { display: flex; align-items: center; gap: 9px; color: #b0bbd0; font-size: 10px; letter-spacing: 2.2px; margin-bottom: 27px; }.eyebrow > span { width: 6px; height: 6px; border-radius: 50%; background: #98e5dc; box-shadow: 0 0 12px #98e5dc; }
h1 { font-size: clamp(40px, 4.1vw, 63px); line-height: 1.35; font-weight: 650; letter-spacing: -2px; }.iridescent { background: linear-gradient(110deg, #9dcaff, #c0b6ff 55%, #8aebe1); background-clip: text; -webkit-text-fill-color: transparent; }.intro-description { margin-top: 22px; color: var(--muted); font-size: 14px; line-height: 1.95; }
.link-art { position: relative; height: 260px; max-width: 490px; margin: 10px 0 13px; }.orbit { position: absolute; border: 1px solid #7282c01c; border-radius: 50%; width: 420px; height: 165px; left: -5px; top: 52px; transform: rotate(-20deg); }.orbit-two { width: 340px; height: 230px; left: 48px; top: 18px; transform: rotate(22deg); }
.glass-loop { position: absolute; width: 194px; height: 113px; border-radius: 65px; border: 19px solid #a4c9ff3d; background: #8fcaff02; box-shadow: inset 3px 3px 7px #ffffff85, inset -5px -5px 14px #081734, 4px 5px 2px #91d3fb5c, -2px -3px 1px #dbf8ffcc, 0 10px 70px #527eff35; transform: rotate(-38deg); }.loop-one { top: 55px; left: 48px; }.loop-two { top: 88px; left: 173px; border-color: #bfb6ff50; box-shadow: inset 3px 3px 7px #ffffff90, inset -5px -5px 14px #29184a, 4px 5px 2px #9f7fee80, -2px -3px 1px #e4dcffbb, 0 10px 70px #ae68ff30; }
.preview-link { position: absolute; top: 175px; left: 102px; display: flex; align-items: center; gap: 13px; padding: 14px 18px; border-radius: 14px; border: 1px solid #cbdcff3d; background: #a7b9ff0d; backdrop-filter: blur(20px); box-shadow: inset 0 1px 0 #ffffff25, 0 15px 35px #0003; color: #bfcce4; font: 12px 'Cascadia Code', Consolas, monospace; white-space: nowrap; }.preview-link b { color: #d8c9ff; font-weight: 500; text-shadow: 0 0 14px #bd94ff; }.preview-dot { width: 6px; height: 6px; background: #8ce5cf; border-radius: 50%; }.art-caption { position: absolute; bottom: 0; left: 0; color: #7c88a3; font-size: 8px; letter-spacing: 3px; }
.intro-features { display: flex; flex-wrap: wrap; gap: 25px; color: #a5b1c9; font-size: 11px; }.intro-features > span { display: inline-flex; align-items: center; gap: 8px; }.intro-features .el-icon { font-size: 15px; color: #9fb6e6; }
.auth-card { position: relative; padding: 38px 35px 26px; border: 1px solid #d0ddff32; border-radius: 28px; background: linear-gradient(145deg, #b9cdff18, #bec9ff08 55%, #a7c1ff13); backdrop-filter: blur(32px) saturate(150%); -webkit-backdrop-filter: blur(32px) saturate(150%); box-shadow: inset 0 1px 0 #ffffff40, inset 1px 0 0 #ffffff0d, 0 30px 80px #02040c66; }.card-glint { position: absolute; top: -1px; left: 35px; right: 35px; height: 1px; background: linear-gradient(90deg, transparent, #c4e3ffe0, transparent); }.card-overline { font-size: 9px; letter-spacing: 1.65px; color: #a3b3d0; }.auth-heading { margin-top: 25px; }h2 { font-size: 30px; letter-spacing: -1px; font-weight: 600; }.heading-dot { color: #a1ceff; }.auth-heading p { color: var(--muted); font-size: 12px; margin-top: 10px; }
.auth-tabs { display: flex; padding: 4px; background: #050a184d; border: 1px solid #b8cfff12; border-radius: 12px; margin: 28px 0 25px; }.auth-tabs button { flex: 1; border: 1px solid transparent; cursor: pointer; color: #9faec8; background: transparent; height: 38px; border-radius: 8px; font: inherit; font-size: 12px; transition: background .2s, color .2s; }.auth-tabs button.active { background: #bfceff1c; color: #eff5ff; border-color: #c9d7ff24; box-shadow: 0 2px 5px #0002; }.auth-tabs button:hover { color: white; }
:deep(.el-form-item) { margin-bottom: 22px; }:deep(.el-form-item__label) { color: #d2daeb; padding-bottom: 8px; line-height: 20px; font-size: 12px; }:deep(.el-input__wrapper) { background: #050b1c38; border-radius: 10px; box-shadow: inset 0 0 0 1px #bacdff26; padding: 4px 13px; min-height: 47px; transition: box-shadow .2s; }:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px #9dbbff; }:deep(.el-input__inner) { color: #f0f5ff; font-size: 12px; }:deep(.el-input__inner::placeholder) { color: #8e9bb6; }:deep(.el-input__icon) { color: #94a5c5; }:deep(.el-form-item__error) { color: #ffb4bf; }
.form-options { display: flex; justify-content: space-between; align-items: center; gap: 5px; margin: -3px 0 20px; }.form-options > span { color: #9daac3; font-size: 10px; }:deep(.el-checkbox__label) { font-size: 11px; color: #bac6dd; }:deep(.el-checkbox__inner) { background: #b2c7ff0f; border-color: #97b2e05e; border-radius: 4px; }:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background: #798dd8; border-color: #798dd8; }:deep(.el-checkbox__input.is-checked + .el-checkbox__label) { color: #c1d2ff; }
.submit-button { width: 100%; height: 48px; border-radius: 11px; border: 1px solid #dce5ff65; background: linear-gradient(110deg, #bdcdff, #b9b4ff 60%, #9acfe0); color: #16203e; font-size: 13px; font-weight: 650; box-shadow: inset 0 1px 0 #ffffff80, 0 4px 24px #99adff25; transition: box-shadow .2s, filter .2s; }.submit-button:hover { color: #101b38; filter: brightness(1.08); box-shadow: 0 5px 30px #a3adff50; }.submit-button .el-icon { margin-left: 14px; font-size: 17px; }.card-footer { margin-top: 27px; padding-top: 21px; border-top: 1px solid #bacdff1a; display: flex; align-items: center; justify-content: center; gap: 7px; font-size: 10px; color: #9cabc6; }.form-row { display: grid; grid-template-columns: 1fr 1.3fr; gap: 12px; min-width: 0; }.form-row > * { min-width: 0; }
.login-footer { color: #8996b0; font-size: 10px; gap: 20px; }.footer-mark { font-size: 8px; letter-spacing: 1.5px; }button:focus-visible, a:focus-visible { outline: 2px solid #bad8ff; outline-offset: 4px; }
@supports not (backdrop-filter: blur(1px)) { .auth-card { background: #1c263d; }.preview-link { background: #202b46; } }
@media (max-width: 1050px) { .login-layout { gap: 40px; grid-template-columns: 1fr 400px; }.auth-card { padding: 32px 27px 24px; }.intro-features { gap: 12px; }.link-art { transform: scale(.85); transform-origin: left center; } }
@media (max-width: 780px) { .login-page { padding: 24px; }.header-note { display: none; }.login-layout { display: flex; flex-direction: column; gap: 32px; margin: 50px auto 45px; min-height: auto; }.intro { text-align: center; }.eyebrow { justify-content: center; font-size: 8px; margin-bottom: 18px; }h1 { font-size: 42px; }.intro-description { font-size: 12px; margin-top: 15px; }.link-art, .intro-features { display: none; }.auth-card { width: 100%; max-width: 440px; }.login-footer { flex-wrap: wrap; justify-content: center; gap: 10px 24px; }.footer-mark { display: none; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation: none !important; transition: none !important; } }
</style>
