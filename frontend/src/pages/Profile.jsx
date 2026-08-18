import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getProfile, updateProfile } from "../api/users";

function validate({ displayName }) {
  const errors = {};
  if (!displayName.trim()) errors.displayName = "El nombre es obligatorio";
  else if (displayName.length > 100) errors.displayName = "Máximo 100 caracteres";
  return errors;
}

function Profile() {
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [displayName, setDisplayName] = useState("");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [baseCurrency, setBaseCurrency] = useState("USD");
  const [timezone, setTimezone] = useState("");

  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getProfile()
      .then((response) => {
        const profile = response.data;
        setDisplayName(profile.displayName || "");
        setAvatarUrl(profile.avatarUrl || "");
        setBaseCurrency(profile.baseCurrency || "USD");
        setTimezone(profile.timezone || "");
      })
      .catch(() => setLoadError("No se pudo cargar tu perfil."))
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    setServerError("");
    setSaved(false);

    const validationErrors = validate({ displayName });
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    setSubmitting(true);
    try {
      await updateProfile({ displayName, avatarUrl, baseCurrency, timezone });
      setSaved(true);
    } catch (error) {
      const status = error.response?.status;
      if (status === 400) {
        setServerError(error.response.data?.details?.join(" ") || "Datos inválidos");
      } else {
        setServerError("No se pudo guardar el perfil. Inténtalo de nuevo.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <main className="new-note">
        <p>Cargando perfil...</p>
      </main>
    );
  }

  return (
    <main className="new-note">
      <h1>Mi perfil</h1>

      {loadError && <p role="alert">{loadError}</p>}

      <form className="note-form" onSubmit={handleSubmit} noValidate>
        <label htmlFor="displayName">Nombre</label>
        <br />
        <input id="displayName" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
        <br />
        {errors.displayName && <p role="alert">{errors.displayName}</p>}

        <label htmlFor="avatarUrl">URL de avatar</label>
        <br />
        <input id="avatarUrl" value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} />
        <br />

        <label htmlFor="baseCurrency">Moneda base</label>
        <br />
        <select id="baseCurrency" value={baseCurrency} onChange={(e) => setBaseCurrency(e.target.value)}>
          <option value="USD">USD</option>
          <option value="EUR">EUR</option>
        </select>
        <br />

        <label htmlFor="timezone">Zona horaria</label>
        <br />
        <input
          id="timezone"
          placeholder="Europe/Madrid"
          value={timezone}
          onChange={(e) => setTimezone(e.target.value)}
        />
        <br />

        {saved && <p>Perfil guardado.</p>}
        {serverError && <p role="alert">{serverError}</p>}

        <button type="submit" className="note-submit btn-primary" disabled={submitting}>
          {submitting ? "Guardando..." : "Guardar cambios"}
        </button>
      </form>

      <p>
        <Link to="/dashboard">Volver al dashboard</Link>
      </p>
    </main>
  );
}

export default Profile;
