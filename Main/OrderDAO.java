// --- OOP INTENT: ABSTRACTION (Module 4) ---
// We use an interface to hide implementation details. 
// This acts as a strict contract. Any class that implements OrderDAO 
// is forced to create a checkout() method, but we don't care HOW they do it here.
public interface OrderDAO {

    // Notice there are no curly braces {} or logic here! Just the method signature.
    void checkout(MenuItem item, int quantityOrdered);

}