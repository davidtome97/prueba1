import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;         // puede ser null si mongo
    private final ProductoMongoRepository productoMongoRepository; // puede ser null si sql
    private final String dbEngine;

    public ProductoService(
            ObjectProvider<ProductoRepository> productoRepository,
            ObjectProvider<ProductoMongoRepository> productoMongoRepository,
            @Value("${app.db.engine:h2}") String dbEngine
    ) {
        this.productoRepository = productoRepository.getIfAvailable();
        this.productoMongoRepository = productoMongoRepository.getIfAvailable();
        this.dbEngine = dbEngine == null ? "h2" : dbEngine.toLowerCase();
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    @Transactional(readOnly = true)
    public List<?> listarTodos() {
        if (isMongo()) return productoMongoRepository.findAll();
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Object buscarPorId(String id) {
        if (isMongo()) {
            return productoMongoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        } else {
            return productoRepository.findById(Long.valueOf(id))
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        }
    }

    @Transactional
    public void guardar(String id, String nombre, String descripcion, double precio) {
        if (isMongo()) {
            ProductoMongo p = (id == null || id.isBlank())
                    ? new ProductoMongo()
                    : productoMongoRepository.findById(id).orElse(new ProductoMongo());

            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);

            productoMongoRepository.save(p);
        } else {
            Producto p = (id == null || id.isBlank())
                    ? new Producto()
                    : productoRepository.findById(Long.valueOf(id)).orElse(new Producto());

            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);

            productoRepository.save(p);
        }
    }

    @Transactional
    public void borrarPorId(String id) {
        if (isMongo()) productoMongoRepository.deleteById(id);
        else productoRepository.deleteById(Long.valueOf(id));
    }
}