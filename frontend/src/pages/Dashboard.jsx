import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getCryptoMarkets } from "../api/cryptos";
import { getMyNotes, deleteNote } from "../api/notes";
import { getWatchlist, addToWatchlist, removeFromWatchlist } from "../api/watchlist";
import Sparkline from "../components/Sparkline";
import BrandTitle from "../components/BrandTitle";
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

function StarButton({ coinId, isWatched, pending, onToggle }) {
  return (
    <button
      type="button"
      className={"star-button" + (isWatched ? " is-watched" : "")}
      disabled={pending}
      onClick={() => onToggle(coinId)}
      aria-label={isWatched ? "Quitar de mi watchlist" : "Añadir a mi watchlist"}
      title={isWatched ? "Quitar de mi watchlist" : "Añadir a mi watchlist"}
    >
      {isWatched ? "★" : "☆"}
    </button>
  );
}

function NotesSummary({ notes, loading, onDelete, deletingId }) {
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
            <button
              type="button"
              className="note-card-delete"
              disabled={deletingId === note.id}
              onClick={() => onDelete(note.id)}
            >
              {deletingId === note.id ? "Eliminando..." : "Eliminar"}
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}

function WatchlistSummary({ watchlist, loading, marketItems, pendingIds, onToggle }) {
  if (loading) return null;
  if (watchlist.length === 0) return null;

  return (
    <section className="notes-summary">
      <h2>Mi watchlist</h2>
      <div className="watchlist-chips">
        {watchlist.map((entry) => {
          const marketItem = marketItems.find((item) => item.coinId === entry.coinId);
          return (
            <span key={entry.coinId} className="watchlist-chip">
              {marketItem?.image && <img src={marketItem.image} alt="" width={18} height={18} />}
              {marketItem ? `${marketItem.name} (${marketItem.symbol.toUpperCase()})` : entry.coinId}
              <button
                type="button"
                className="watchlist-chip-remove"
                disabled={pendingIds.has(entry.coinId)}
                onClick={() => onToggle(entry.coinId)}
                aria-label={`Quitar ${entry.coinId} de mi watchlist`}
              >
                ×
              </button>
            </span>
          );
        })}
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
  const [deletingId, setDeletingId] = useState(null);

  const [watchlist, setWatchlist] = useState([]);
  const [watchlistLoading, setWatchlistLoading] = useState(true);
  const [pendingWatchlistIds, setPendingWatchlistIds] = useState(new Set());

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
    const intervalId = setInterval(() => {
      getCryptoMarkets({ page, perPage: PER_PAGE, currency: "usd" })
        .then((response) => setItems(response.data.items))
        .catch(() => {
          // Silent: keep showing the last known-good data instead of
          // disrupting the view for a transient background refresh failure.
        });
    }, 60000);

    return () => clearInterval(intervalId);
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

  useEffect(() => {
    let cancelled = false;
    getWatchlist()
      .then((response) => {
        if (!cancelled) setWatchlist(response.data.items);
      })
      .catch(() => {
        if (!cancelled) setWatchlist([]);
      })
      .finally(() => {
        if (!cancelled) setWatchlistLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleDeleteNote(noteId) {
    if (!window.confirm("¿Seguro que quieres eliminar esta nota? Esta acción no se puede deshacer.")) {
      return;
    }

    setDeletingId(noteId);
    try {
      await deleteNote(noteId);
      setNotes((prev) => prev.filter((note) => note.id !== noteId));
    } catch {
      window.alert("No se pudo eliminar la nota. Inténtalo de nuevo.");
    } finally {
      setDeletingId(null);
    }
  }

  async function handleToggleWatchlist(coinId) {
    const isWatched = watchlist.some((entry) => entry.coinId === coinId);
    setPendingWatchlistIds((prev) => new Set(prev).add(coinId));

    try {
      if (isWatched) {
        await removeFromWatchlist(coinId);
        setWatchlist((prev) => prev.filter((entry) => entry.coinId !== coinId));
      } else {
        const response = await addToWatchlist(coinId);
        setWatchlist((prev) => [response.data, ...prev.filter((entry) => entry.coinId !== coinId)]);
      }
    } catch {
      window.alert("No se pudo actualizar la watchlist. Inténtalo de nuevo.");
    } finally {
      setPendingWatchlistIds((prev) => {
        const next = new Set(prev);
        next.delete(coinId);
        return next;
      });
    }
  }

  const watchedCoinIds = new Set(watchlist.map((entry) => entry.coinId));

  return (
    <>
    <main className="market">
      <div className="dashboard-header">
        <Link to="/profile" className="btn dashboard-header-profile">
          Mi perfil
        </Link>
        <BrandTitle />
        <Link to="/notes/new" className="btn btn-primary btn-primary-round">
          Crear nota
        </Link>
      </div>

      <NotesSummary
        notes={notes}
        loading={notesLoading}
        onDelete={handleDeleteNote}
        deletingId={deletingId}
      />

      <WatchlistSummary
        watchlist={watchlist}
        loading={watchlistLoading}
        marketItems={items}
        pendingIds={pendingWatchlistIds}
        onToggle={handleToggleWatchlist}
      />

      {loading && <p>Cargando datos de mercado...</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && (
        <>
          <div className="market-table-wrapper">
            <table className="market-table">
              <thead>
                <tr>
                  <th></th>
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
                    <td>
                      <StarButton
                        coinId={item.coinId}
                        isWatched={watchedCoinIds.has(item.coinId)}
                        pending={pendingWatchlistIds.has(item.coinId)}
                        onToggle={handleToggleWatchlist}
                      />
                    </td>
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

          <div className="market-cards">
            {items.map((item, index) => (
              <article key={item.coinId} className="market-card">
                <div className="market-card-top">
                  <span className="market-card-rank">{(page - 1) * PER_PAGE + index + 1}</span>
                  <img src={item.image} alt="" width={28} height={28} />
                  <div className="market-card-name">
                    <span>{item.name}</span>
                    <span className="market-symbol">{item.symbol}</span>
                  </div>
                  <StarButton
                    coinId={item.coinId}
                    isWatched={watchedCoinIds.has(item.coinId)}
                    pending={pendingWatchlistIds.has(item.coinId)}
                    onToggle={handleToggleWatchlist}
                  />
                </div>
                <div className="market-card-sparkline">
                  <Sparkline data={item.sparkline7d} width={280} height={40} />
                </div>
                <div className="market-card-bottom">
                  <span className="market-price">
                    {item.currentPrice != null ? priceFormatter.format(item.currentPrice) : "—"}
                  </span>
                  <span
                    className={
                      "market-change " +
                      (item.priceChangePercentage24h >= 0 ? "market-change-up" : "market-change-down")
                    }
                  >
                    {item.priceChangePercentage24h != null
                      ? changeFormatter.format(item.priceChangePercentage24h / 100)
                      : "—"}
                  </span>
                </div>
              </article>
            ))}
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
