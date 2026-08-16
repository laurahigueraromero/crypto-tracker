import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getCryptoMarkets } from "../api/cryptos";
import { getMyNotes } from "../api/notes";
import Sparkline from "../components/Sparkline";
import { useAuth } from "../context/AuthContext";
import { NOTE_TYPE_LABELS } from "../constants/noteTypes";

const PER_PAGE = 20;

const priceFormatter = new Intl.NumberFormat("es-ES", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 6,
});

const changeFormatter = new Intl.NumberFormat("es-ES", {
  style: "percent",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  signDisplay: "always",
});

function NotesSummary({ notes, loading }) {
  if (loading) return null;
  if (notes.length === 0) return null;

  return (
    <section className="notes-summary">
      <h2>Mis notas</h2>
      <div className="notes-summary-list">
        {notes.map((note) => (
          <article key={note.id} className="note-card">
            <div className="note-card-header">
              <strong>{note.title}</strong>
              <span className="note-card-type">{NOTE_TYPE_LABELS[note.type] || note.type}</span>
            </div>
            <p className="note-card-content">{note.content}</p>
            <div className="note-card-coins">
              {note.coinIds.map((coinId) => (
                <span key={coinId} className="note-card-coin">
                  {coinId}
                </span>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function Dashboard() {
  const { logout } = useAuth();
  const [page, setPage] = useState(1);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [notes, setNotes] = useState([]);
  const [notesLoading, setNotesLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");

    getCryptoMarkets({ page, perPage: PER_PAGE, currency: "usd" })
      .then((response) => {
        if (!cancelled) setItems(response.data.items);
      })
      .catch(() => {
        if (!cancelled) setError("Datos de mercado no disponibles en este momento.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [page]);

  useEffect(() => {
    let cancelled = false;
    getMyNotes()
      .then((response) => {
        if (!cancelled) setNotes(response.data);
      })
      .catch(() => {
        if (!cancelled) setNotes([]);
      })
      .finally(() => {
        if (!cancelled) setNotesLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
    <main className="market">
      <div className="dashboard-header">
        <h1>Crypto Tracker</h1>
        <Link to="/notes/new" className="btn btn-primary btn-primary-round">
          Crear nota
        </Link>
      </div>

      <NotesSummary notes={notes} loading={notesLoading} />

      {loading && <p>Cargando datos de mercado...</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && (
        <>
          <div className="market-table-wrapper">
            <table className="market-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Moneda</th>
                  <th>Precio</th>
                  <th>24h</th>
                  <th>Últimos 7 días</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, index) => (
                  <tr key={item.coinId}>
                    <td>{(page - 1) * PER_PAGE + index + 1}</td>
                    <td>
                      <div className="market-coin">
                        <img src={item.image} alt="" width={24} height={24} />
                        <span>{item.name}</span>
                        <span className="market-symbol">{item.symbol}</span>
                      </div>
                    </td>
                    <td className="market-price">
                      {item.currentPrice != null ? priceFormatter.format(item.currentPrice) : "—"}
                    </td>
                    <td
                      className={
                        "market-change " +
                        (item.priceChangePercentage24h >= 0 ? "market-change-up" : "market-change-down")
                      }
                    >
                      {item.priceChangePercentage24h != null
                        ? changeFormatter.format(item.priceChangePercentage24h / 100)
                        : "—"}
                    </td>
                    <td>
                      <Sparkline data={item.sparkline7d} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="market-pagination">
            <button type="button" disabled={page === 1} onClick={() => setPage((p) => p - 1)}>
              Anterior
            </button>
            <span>Página {page}</span>
            <button type="button" onClick={() => setPage((p) => p + 1)}>
              Siguiente
            </button>
          </div>
        </>
      )}
    </main>
    <footer className="page-footer">
      <button type="button" onClick={logout}>
        Cerrar sesión
      </button>
    </footer>
    </>
  );
}

export default Dashboard;
