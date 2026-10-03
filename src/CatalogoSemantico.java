package semantic;

/**
 * Contrato de consulta publicado para los módulos semánticos.
 * Persona 3 puede implementar/adaptar este contrato con su catálogo real.
 */
public interface CatalogoSemantico {
    TablaCatalogo buscarTabla(String nombre);
}
