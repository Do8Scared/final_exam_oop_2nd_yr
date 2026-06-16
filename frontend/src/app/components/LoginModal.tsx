import { useState } from "react";
import { X, User, Mail, Lock, Eye, EyeOff, LogIn, UserPlus } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

type LoginModalProps = {
  onClose: () => void;
  onLogin: (user: { name: string; email: string }) => void;
};

export function LoginModal({ onClose, onLogin }: LoginModalProps) {
  const [tab, setTab] = useState<"login" | "register">("login");
  const [showPass, setShowPass] = useState(false);
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [errors, setErrors] = useState<typeof form>({ name: "", email: "", password: "", confirm: "" });

  const update = (k: keyof typeof form, v: string) => setForm((f) => ({ ...f, [k]: v }));

  const validate = () => {
    const e = { name: "", email: "", password: "", confirm: "" };
    if (tab === "register" && !form.name.trim()) e.name = "Name is required";
    if (!form.email.trim()) e.email = "Email is required";
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = "Enter a valid email";
    if (!form.password) e.password = "Password is required";
    else if (form.password.length < 6) e.password = "At least 6 characters";
    if (tab === "register" && form.password !== form.confirm) e.confirm = "Passwords do not match";
    setErrors(e);
    return !Object.values(e).some(Boolean);
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    
    try {
      if (tab === "register") {
        const res = await fetch("http://localhost:8081/api/auth/register", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name: form.name, email: form.email, password: form.password })
        });
        if (res.ok) {
          alert("Registration successful! You can now log in.");
          setTab("login");
          setForm(f => ({...f, password: "", confirm: ""}));
        } else {
          const data = await res.json();
          setErrors(e => ({...e, email: data.error || "Registration failed"}));
        }
      } else {
        const res = await fetch("http://localhost:8081/api/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: form.email, password: form.password })
        });
        if (res.ok) {
          const user = await res.json();
          onLogin({ name: user.name, email: user.email });
          onClose();
        } else {
          const data = await res.json();
          setErrors(e => ({...e, password: data.error || "Invalid credentials"}));
        }
      }
    } catch (err) {
      console.error("Auth error:", err);
      alert("Cannot connect to server.");
    }
  };

  const inputField = (
    label: string,
    key: keyof typeof form,
    type: string,
    placeholder: string,
    icon: React.ReactNode,
    suffix?: React.ReactNode
  ) => (
    <div className="flex flex-col gap-1">
      <label style={{ color: "#8a8070", fontSize: "0.75rem", letterSpacing: "0.08em", fontFamily: "'Oswald', sans-serif" }}>
        {label}
      </label>
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: "#c8932a" }}>{icon}</span>
        <input
          type={type}
          value={form[key]}
          onChange={(e) => update(key, e.target.value)}
          placeholder={placeholder}
          onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
          className="w-full pl-10 pr-10 py-3 rounded-lg outline-none transition-all"
          style={{
            background: "#242320",
            border: `1px solid ${errors[key] ? "#c0392b" : "rgba(200,147,42,0.2)"}`,
            color: "#f0ede8",
            fontFamily: "'Inter', sans-serif",
            fontSize: "0.9rem",
          }}
        />
        {suffix && <span className="absolute right-3 top-1/2 -translate-y-1/2">{suffix}</span>}
      </div>
      {errors[key] && <span style={{ color: "#c0392b", fontSize: "0.75rem" }}>{errors[key]}</span>}
    </div>
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0"
        style={{ background: "rgba(17,17,16,0.85)", backdropFilter: "blur(8px)" }}
        onClick={onClose}
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.96, y: 16 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 16 }}
        className="relative w-full max-w-md rounded-2xl overflow-hidden"
        style={{ background: "#1c1b19", border: "1px solid rgba(200,147,42,0.2)" }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top accent bar */}
        <div className="h-1 w-full" style={{ background: "linear-gradient(to right, #c8932a, #f0c060, #c8932a)" }} />

        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-6 pb-4">
          <div>
            <p style={{ fontFamily: "'Oswald', sans-serif", color: "#c8932a", fontSize: "0.75rem", letterSpacing: "0.2em" }}>GARAHE NI MATEICLA</p>
            <h2 style={{ fontFamily: "'Oswald', sans-serif", color: "#f0ede8", fontSize: "1.6rem", letterSpacing: "0.04em", marginTop: "2px" }}>
              {tab === "login" ? "WELCOME BACK" : "CREATE ACCOUNT"}
            </h2>
          </div>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Tabs */}
        <div className="flex mx-6 rounded-lg overflow-hidden" style={{ background: "#242320", border: "1px solid rgba(200,147,42,0.15)" }}>
          {(["login", "register"] as const).map((t) => (
            <button
              key={t}
              onClick={() => { setTab(t); setErrors({ name: "", email: "", password: "", confirm: "" }); }}
              className="flex-1 py-2.5 flex items-center justify-center gap-2 transition-all"
              style={{
                background: tab === t ? "#c8932a" : "transparent",
                color: tab === t ? "#111110" : "#8a8070",
                fontFamily: "'Oswald', sans-serif",
                letterSpacing: "0.06em",
                fontSize: "0.85rem",
              }}
            >
              {t === "login" ? <LogIn size={14} /> : <UserPlus size={14} />}
              {t === "login" ? "LOG IN" : "REGISTER"}
            </button>
          ))}
        </div>

        {/* Form */}
        <div className="px-6 py-5 space-y-4">
          <AnimatePresence mode="wait">
            <motion.div
              key={tab}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.2 }}
              className="space-y-4"
            >
              {tab === "register" &&
                inputField("FULL NAME", "name", "text", "Juan dela Cruz", <User size={15} />)}
              {inputField("EMAIL ADDRESS", "email", "email", "juan@email.com", <Mail size={15} />)}
              {inputField(
                "PASSWORD",
                "password",
                showPass ? "text" : "password",
                "••••••••",
                <Lock size={15} />,
                <button type="button" onClick={() => setShowPass((s) => !s)} style={{ color: "#8a8070" }}>
                  {showPass ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              )}
              {tab === "register" &&
                inputField("CONFIRM PASSWORD", "confirm", showPass ? "text" : "password", "••••••••", <Lock size={15} />)}
            </motion.div>
          </AnimatePresence>

          {tab === "login" && (
            <div className="text-right">
              <button style={{ color: "#c8932a", fontSize: "0.8rem", fontFamily: "'Inter', sans-serif" }}>
                Forgot password?
              </button>
            </div>
          )}

          <button
            onClick={handleSubmit}
            className="w-full py-4 rounded-lg flex items-center justify-center gap-2 transition-all hover:brightness-110 active:scale-[0.98] mt-2"
            style={{ background: "#c8932a", color: "#111110", fontFamily: "'Oswald', sans-serif", letterSpacing: "0.08em" }}
          >
            {tab === "login" ? <><LogIn size={16} /> LOG IN</> : <><UserPlus size={16} /> CREATE ACCOUNT</>}
          </button>

          <p style={{ color: "#8a8070", fontSize: "0.8rem", textAlign: "center", fontFamily: "'Inter', sans-serif" }}>
            {tab === "login" ? "Don't have an account? " : "Already have an account? "}
            <button
              onClick={() => { setTab(tab === "login" ? "register" : "login"); setErrors({ name: "", email: "", password: "", confirm: "" }); }}
              style={{ color: "#c8932a" }}
            >
              {tab === "login" ? "Register" : "Log in"}
            </button>
          </p>
        </div>
      </motion.div>
    </div>
  );
}
