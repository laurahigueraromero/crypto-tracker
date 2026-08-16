import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../api/auth";
import { useAuth } from "../context/AuthContext";

function Login() {
  const navigate = useNavigate();
  const { login: setSession } = useAuth();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      const response = await login(form);
      setSession(response.data);
      navigate("/dashboard");
    } catch (err) {
      const status = err.response?.status;
      if (status === 401) {
        setError("Email o contraseña incorrectos");
      } else if (status === 423) {
        setError("Cuenta bloqueada temporalmente por varios intentos fallidos");
      } else {
        setError("No se pudo iniciar sesión. Inténtalo de nuevo.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <form onSubmit={handleSubmit} noValidate>
        <h1>Iniciar sesión</h1>

        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          value={form.email}
          onChange={handleChange}
        />

        <label htmlFor="password">Contraseña</label>
        <input
          id="password"
          name="password"
          type="password"
          value={form.password}
          onChange={handleChange}
        />

        {error && <p role="alert">{error}</p>}

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? "Entrando..." : "Entrar"}
        </button>

        <p>
          ¿No tienes cuenta? <Link to="/register">Regístrate</Link>
        </p>
      </form>
    </main>
  );
}

export default Login;
