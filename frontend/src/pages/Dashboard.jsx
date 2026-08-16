import { useAuth } from "../context/AuthContext";

function Dashboard() {
  const { logout } = useAuth();

  return (
    <main>
      <h1>Área privada</h1>
      <p>Próximamente: estadísticas de mercado, watchlist y notas.</p>
      <button type="button" onClick={logout}>
        Cerrar sesión
      </button>
    </main>
  );
}

export default Dashboard;
