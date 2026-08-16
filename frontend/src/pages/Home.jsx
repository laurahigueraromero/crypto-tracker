import { Link } from "react-router-dom";
import BrandTitle from "../components/BrandTitle";

function Home() {
  return (
    <main>
      <BrandTitle />
      <p>
        <Link to="/register">Crear cuenta</Link> · <Link to="/login">Iniciar sesión</Link>
      </p>
    </main>
  );
}

export default Home;
