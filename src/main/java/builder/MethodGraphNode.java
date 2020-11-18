package builder;

import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;

public class MethodGraphNode extends MethodNode {

    private boolean used;
    private boolean visited;
    Set<MethodGraphNode> calledMethods = new HashSet<>();
    String owner;

    public MethodGraphNode(int access, String owner, String name, String desc, String signature, String[] exceptions) {

        super(ASM6, access, name, desc, signature, exceptions);
        this.owner = owner;
        used = false;
        visited = false;
    }

    public void markAsUsed(){

        used = true;
    }

    public void markAsVisited(){

        visited = true;
    }

    public boolean isUsed(){

        return used;
    }

    public boolean isVisited(){

        return visited;
    }



    @Override
    public boolean equals(Object obj){
        if (obj instanceof MethodGraphNode){
            MethodGraphNode node = (MethodGraphNode) obj;
            return owner == node.owner && name == node.name && desc == node.desc;
        }
        return false;
    }

}
