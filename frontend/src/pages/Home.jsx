import { Link } from "react-router-dom";

function Home() {
  return (
    <main>
      <h1>Crypto Tracker</h1>
      <p>
        <Link to="/register">Crear cuenta</Link> · <Link to="/login">Iniciar sesión</Link>
      </p>
    </main>
  );
}

export default Home;
