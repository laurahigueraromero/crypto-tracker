import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "../api/auth";

const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

function validate({ email, password, displayName }) {
  const errors = {};
  if (!email.trim()) errors.email = "El email es obligatorio";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = "El email no es válido";

  if (!password) errors.password = "La contraseña es obligatoria";
  else if (!PASSWORD_PATTERN.test(password))
    errors.password = "Debe tener 8+ caracteres, una mayúscula y un número";

  if (!displayName.trim()) errors.displayName = "El nombre es obligatorio";
  else if (displayName.length > 100) errors.displayName = "Máximo 100 caracteres";

  return errors;
}

function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "", displayName: "" });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setServerError("");

    const validationErrors = validate(form);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    setSubmitting(true);
    try {
      await register(form);
      navigate("/login");
    } catch (error) {
      const status = error.response?.status;
      if (status === 409) {
        setServerError("Ese email ya está registrado");
      } else if (status === 400) {
        setServerError(error.response.data?.details?.join(" ") || "Datos inválidos");
      } else {
        setServerError("No se pudo completar el registro. Inténtalo de nuevo.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <form onSubmit={handleSubmit} noValidate>
        <h1>Crear cuenta</h1>

        <label htmlFor="displayName">Nombre</label>
        <input
          id="displayName"
          name="displayName"
          value={form.displayName}
          onChange={handleChange}
        />
        {errors.displayName && <p role="alert">{errors.displayName}</p>}

        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          value={form.email}
          onChange={handleChange}
        />
        {errors.email && <p role="alert">{errors.email}</p>}

        <label htmlFor="password">Contraseña</label>
        <input
          id="password"
          name="password"
          type="password"
          value={form.password}
          onChange={handleChange}
        />
        {errors.password && <p role="alert">{errors.password}</p>}

        {serverError && <p role="alert">{serverError}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? "Creando cuenta..." : "Registrarme"}
        </button>

        <p>
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </form>
    </main>
  );
}

export default Register;
