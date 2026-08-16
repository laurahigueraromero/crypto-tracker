import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getCryptoMarkets } from "../api/cryptos";
import { createNote } from "../api/notes";
import { NOTE_TYPES } from "../constants/noteTypes";

function validate({ title, content, coinIds }) {
  const errors = {};
  if (!title.trim()) errors.title = "El título es obligatorio";
  else if (title.length > 100) errors.title = "Máximo 100 caracteres";

  if (!content.trim()) errors.content = "El contenido es obligatorio";
  else if (content.length > 2000) errors.content = "Máximo 2000 caracteres";

  if (coinIds.length === 0) errors.coinIds = "Selecciona al menos una criptomoneda";

  return errors;
}

function NewNote() {
  const navigate = useNavigate();
  const [coins, setCoins] = useState([]);
  const [loadingCoins, setLoadingCoins] = useState(true);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [type, setType] = useState(NOTE_TYPES[0].value);
  const [coinIds, setCoinIds] = useState([]);
  const [tagsInput, setTagsInput] = useState("");
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCryptoMarkets({ page: 1, perPage: 20 })
      .then((response) => setCoins(response.data.items))
      .catch(() => setServerError("No se pudo cargar la lista de criptomonedas."))
      .finally(() => setLoadingCoins(false));
  }, []);

  function toggleCoin(coinId) {
    setCoinIds((prev) =>
      prev.includes(coinId) ? prev.filter((id) => id !== coinId) : [...prev, coinId]
    );
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setServerError("");

    const validationErrors = validate({ title, content, coinIds });
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    const tags = tagsInput
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean);

    setSubmitting(true);
    try {
      await createNote({ title, content, type, coinIds, tags });
      navigate("/dashboard");
    } catch (error) {
      const status = error.response?.status;
      if (status === 400) {
        setServerError(error.response.data?.details?.join(" ") || "Datos inválidos");
      } else {
        setServerError("No se pudo crear la nota. Inténtalo de nuevo.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="new-note">
      <h1>Nueva nota</h1>

      <form className="note-form" onSubmit={handleSubmit} noValidate>
        <label htmlFor="title">Título</label>
        <br />
        <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <br />
        {errors.title && <p role="alert">{errors.title}</p>}

        <label htmlFor="content">Contenido</label>
        <br />
        <textarea
          id="content"
          rows={5}
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
        <br />
        {errors.content && <p role="alert">{errors.content}</p>}

        <label htmlFor="type">Tipo</label>
        <br />
        <select id="type" value={type} onChange={(e) => setType(e.target.value)}>
          {NOTE_TYPES.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <br />

        <label htmlFor="tags">Tags (separados por comas)</label>
        <br />
        <input id="tags" value={tagsInput} onChange={(e) => setTagsInput(e.target.value)} />
        <br />

        <fieldset className="coin-fieldset">
          <legend>Criptomonedas asociadas</legend>
          {loadingCoins && <p>Cargando criptomonedas...</p>}
          {!loadingCoins && (
            <div className="coin-checkbox-grid">
              {coins.map((coin) => (
                <label key={coin.coinId} className="coin-checkbox">
                  <input
                    type="checkbox"
                    checked={coinIds.includes(coin.coinId)}
                    onChange={() => toggleCoin(coin.coinId)}
                  />
                  {coin.name} ({coin.symbol.toUpperCase()})
                </label>
              ))}
            </div>
          )}
        </fieldset>
        {errors.coinIds && <p role="alert">{errors.coinIds}</p>}

        {serverError && <p role="alert">{serverError}</p>}

        <button type="submit" className="note-submit" disabled={submitting}>
          {submitting ? "Guardando..." : "Guardar nota"}
        </button>
      </form>
    </main>
  );
}

export default NewNote;
