package ai.verum.theleveler.core

/**
 * Index map for fast statement lookup.
 * Optional utility for large document sets.
 */
class StatementIndex {
    
    private val byId = mutableMapOf<String, Statement>()
    private val byActor = mutableMapOf<String, MutableList<Statement>>()
    private val bySource = mutableMapOf<String, MutableList<Statement>>()
    
    private var idCounter = 0
    
    /**
     * Add a statement to the index.
     */
    fun add(statement: Statement): String {
        val id = "stmt_${++idCounter}"
        byId[id] = statement
        
        byActor.getOrPut(statement.actor.normalized) { mutableListOf() }.add(statement)
        
        statement.sourceId?.let { source ->
            bySource.getOrPut(source) { mutableListOf() }.add(statement)
        }
        
        return id
    }
    
    /**
     * Add multiple statements.
     */
    fun addAll(statements: List<Statement>): List<String> {
        return statements.map { add(it) }
    }
    
    /**
     * Get statement by ID.
     */
    fun get(id: String): Statement? = byId[id]
    
    /**
     * Get all statements by actor.
     */
    fun getByActor(actorNormalized: String): List<Statement> {
        return byActor[actorNormalized] ?: emptyList()
    }
    
    /**
     * Get all statements from a source.
     */
    fun getBySource(sourceId: String): List<Statement> {
        return bySource[sourceId] ?: emptyList()
    }
    
    /**
     * Get all statements.
     */
    fun getAll(): List<Statement> = byId.values.toList()
    
    /**
     * Get all actors.
     */
    fun getActors(): Set<String> = byActor.keys
    
    /**
     * Get all sources.
     */
    fun getSources(): Set<String> = bySource.keys
    
    /**
     * Get count of statements.
     */
    fun size(): Int = byId.size
    
    /**
     * Clear the index.
     */
    fun clear() {
        byId.clear()
        byActor.clear()
        bySource.clear()
        idCounter = 0
    }
}
