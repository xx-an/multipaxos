package lsr.paxos.storage;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

import scala.collection.mutable.ListBuffer

import lsr.paxos.core.MPLib._;
import lsr.paxos.core.MultiPaxosImpl._;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains data related with one consensus instance.
 */
/**
     * Initializes new instance of consensus with all value specified.
     * 
     * @param id - the id of instance to create
     * @param state - the state of consensus
     * @param view - the view of last message in this consensus
     * @param value - the value accepted or decided in this instance
     */
class ConsensusInstance(id : Integer, status : LogEntryState, ballot : Integer, v : Array[Byte]) extends Serializable 
{
    private final val serialVersionUID : Long = 1L;
    
    final protected var inst : Integer = id;
    protected var view : Integer = ballot;
    protected var value : Array[Byte] = v;
    protected var state : LogEntryState = status;

    @transient protected var accepts : BitSet = new BitSet();
    
    @transient protected var decidable : Boolean = false;
    
    /**
     * Initializes new empty instance of consensus. The initial state is set to
     * <code>UNKNOWN</code>, view to <code>-1</code> and value to
     * <code>null</code>.
     * 
     * @param id the id of instance to create
     */
    
    def this(id : Integer) {
      this(id, LogEntryState.UNKNOWN, 0, null);
    }
    
    /**
     * Initializes new instance of consensus from input stream. The input stream
     * should contain serialized instance created by <code>toByteArray()</code>
     * or <code>write(ByteBuffer)</code> method.
     * 
     * @param input - the input stream containing serialized consensus instance
     * @throws IOException the stream has been closed and the contained input
     *             stream does not support reading after close, or another I/O
     *             error occurs
     * @see #toByteArray()
     * @see #write(ByteBuffer)
     */
    
    @throws(classOf[IOException])
    def read(input: DataInputStream) : ConsensusInstance = {
        this.inst = input.readInt();
        this.view = input.readInt();
        this.state = LogEntryState.values()(input.readInt());

        var size = input.readInt();
        if (size == -1) {
            this.value = null;
        } else {
            this.value = new Array[Byte](size);
            input.readFully(this.value);
        }
        return this;
    }
    
    def readConsensus(input: consensus_ext[Array[Byte], Unit]) = {
        this.inst = def_getIns(input);
        this.view = def_getView(input);
        this.state = LogEntryState.values()(def_getStatus(input));
        this.value = def_getValue(input) match {
          case Some(Comd(v)) => v
          case _ => null
        }
        if(accepts != null)
        {
          val accs = def_getAccepts(input);
          for (i <- accs) 
            this.accepts.set(i);
        }
    }
    
    /**
     * Construct a consensus_ext class from the ConsensusInstance class.
     * 
     * @param nas - the number of the replicas
     */
    def getConsensus(nas : Integer) : consensus_ext[Array[Byte], Unit] =
    {
        val status = (if (state == LogEntryState.UNKNOWN) 0 
          else if(state == LogEntryState.KNOWN) 1
          else 2);
        val v = (if (value == null) None else (Some(Comd(value))));
        val accs = new ListBuffer[Integer]()
        if(accepts != null)
        {
          for (i <- 0 until nas)
          {
            if (accepts.get(i))
            {
              accs += i
            }
          }
        }
        return consensus_exta[Array[Byte], Unit](inst, view, accs.toList, status, v, Unit);
    }

    /**
     * Gets the number of the consensus instance. Different instances should
     * have different id's.
     * 
     * @return id of instance
     */
    def getInst() : Integer = {
        return inst;
    }

    /**
     * Changes the view to the newest one. It cannot be changed to value less
     * than current view, and shouldn't be changed if the consensus is already
     * in <code>Decided</code> state.
     * 
     * Clears the accepts if the new view is higher than the current one.
     * 
     * @param view - the new view value
     */
    def setView(view : Integer) = {
        assert(this.view <= view, "Cannot set smaller view.");
        assert(state != LogEntryState.DECIDED || view == this.view);
        if (this.view < view) {
            accepts.clear();
            this.view = view;
        }
    }
    
    def setState(newState : LogEntryState) = {
      state = newState;
    }

    /**
     * Gets the current view of this instance. The view of instance is
     * represented by the view of last message. If the current state of
     * consensus is decided, then view should not be changed.
     * 
     * @return the view number of this instance
     */
    def getView() : Integer = {
        return view;
    }

    /**
     * Sets new value holding by this instance. Each value has view in which it
     * is valid, so it has to be set here also.
     * 
     * @param view - the view number in which value is valid
     * @param value - the value which was accepted by this instance
     */
    def setValue(view : Integer, value : Array[Byte]) : Unit = {
    	if (view < this.view) {
            return;
        }

        if (state == LogEntryState.UNKNOWN) {
            state = LogEntryState.KNOWN;
        }

        if (state == LogEntryState.DECIDED && !Arrays.equals(this.value, value)) {
            throw new RuntimeException("Cannot change values on a decided instance: " + this);
        }

        if (view > this.view) {
            // Higher view value. Accept any value.
            this.view = view;
        } else {
            assert(this.view == view);
            // Same view. Accept a value only if the current value is null
            // or if the current value is equal to the new value.
            assert(this.value == null || (value == this.value), "Different value for the same view");
        }

        this.value = value;
    }

    /**
     * Returns the value holding by this consensus. It represents last value
     * which was accepted by <code>Acceptor</code>.
     * 
     * @return the current value of this instance
     */
    def getValue() : Array[Byte] = {
        return value;
    }

    /**
     * Gets the current state of this instance. When the state is set to
     * <code>DECIDED</code> no values should be changed.
     * 
     * @return current state of consensus instance
     */
    def getState() : LogEntryState = {
        return state;
    }

    /**
     * Gets the set of replicas from which we get the <code>ACCEPT</code>
     * message from the current <code>view</code>.
     * 
     * @return id's of replicas
     */
    def getAccepts() : BitSet = {
        return accepts;
    }

    /** Returns if the instances is accepted by the majority */
    def isMajority(nas : Integer) : Boolean = {
        return accepts.cardinality() * 2 > nas;
    }

    /**
     * Changes the current state of this instance to <code>DECIDED</code>. This
     * instance cannot be changed so <code>accepts</code> value will be set to
     * <code>null</code>.
     * 
     * @see #getAccepts()
     */
    def setDecided() = {
        assert(value != null);
        state = LogEntryState.DECIDED;
        accepts = null;
    }

    /**
     * Serializes and writes this consensus instance to specified byte buffer.
     * Specified byte buffer requires at least <code>byteSize()</code> remaining
     * size.
     * 
     * @param byteBuffer - the buffer where serialized consensus instance will
     *            be written
     * @see #byteSize()
     */
    def write(byteBuffer : ByteBuffer) = {
        byteBuffer.putInt(id);
        byteBuffer.putInt(view);
        byteBuffer.putInt(state.ordinal());
        if (value == null) {
            byteBuffer.putInt(-1);
        } else {
            byteBuffer.putInt(value.length);
            byteBuffer.put(value);
        }
    }

    /**
     * Returns size of serialized instance in bytes. This value is equal to
     * length of array returned by <code>toByteArray()</code> method and number
     * of bytes written to <code>ByteBuffer</code> using
     * <code>write(ByteBuffer)</code> method.
     * 
     * @return size of serialized instance
     */
    def byteSize() : Integer = {
        var size : Integer = (if (value == null) 0 else value.length) + 4 /* length of array */;
        size += 3 * 4 /* ID, view and state */;
        return size;
    }

    override def hashCode : Int =  {
        val prime : Int = 31;
        var result = 1;
        result = prime * result + id;
        result = prime * result + (if (state == null) 0 else state.hashCode());
        result = prime * result + value.hashCode();
        result = prime * result + view;
        return result;
    }

    override def equals(other: Any) = other match {
      case that:ConsensusInstance => (that.isInstanceOf[ConsensusInstance]) && 
        this.inst == that.inst && this.view == that.view && this.state == that.state && this.value == that.value && this.accepts == that.accepts
      case _ => false
    }

    override def toString = {
        "(" + id + ", " + state + ", view=" + view + ", value=" + value + ")";
    }

    /** Called when received a higher view Accept */
    def reset() = {
        accepts.clear();
        state = LogEntryState.UNKNOWN;
        view = 0;
        value = null;
    }

    /**
     * Ignores any update with a view lower than the local one.
     * 
     * @param newView
     * @param newValue
     */
    def updateStateFromKnown(newView : Integer, newValue : Array[Byte]) : Unit = {
        // Ignore any state update from an older view.
        if (newView < view) {
            return;
        }
        state match {
            case LogEntryState.DECIDED => {
                /*
                 * This can happen when the new leader re-proposes an instance
                 * that was decided by some processes on a previous view.
                 * 
                 * The value must be the same as the local value.
                 */
                assert(Arrays.equals(newValue, value), "Values don't match. New view: " + newView +
                                                        ", local: " + this + ", newValue: " +
                                                        Arrays.toString(newValue) + ", old: " +
                                                        Arrays.toString(value));}

            case LogEntryState.KNOWN => {
                // The message is for a higher view or same view and same value.
                // State remains in known
                assert(newView != view || (newValue == value), "Values don't match. newView: " +
                                                                           newView +
                                                                           ", local: " +
                                                                           this);
                setValue(newView, newValue);}

            case LogEntryState.UNKNOWN =>
                {setValue(newView, newValue);}

            case _ =>
                throw new RuntimeException("Unknown instance state");
        }
    }

    /**
     * Update the local state from an already decided instance. This differs
     * from {@link #updateStateFromKnown(int, byte[])} in that it will accept
     * messages from lower views.
     * 
     * Used during catchup or view change, when the replica may receive messages
     * from lower views that are decided.
     * 
     * State is set to known, as the instance is decided from other class
     * 
     * This method does not check if args are valid, as this is unnecessary in
     * this case.
     * 
     * @param newView
     * @param newValue
     */
    def updateStateFromDecision(newView : Integer, newValue : Array[Byte]) : Unit = {
        assert(newValue != null);
        if (state == LogEntryState.DECIDED) {
            logger.error("Updating a decided instance from a catchup message: {}", this);
            // The value must be the same as the local value. No change.
            assert (newValue == value, "Values don't match. New view: " + newView +
                                                    ", local: " + this);
            return;
        } else {
            this.view = newView;
            this.value = newValue;
            this.state = LogEntryState.KNOWN;
        }
    }
    
    /**
     * If the instance is ready to be decided, but misses batch values it is
     * decidable. Catch-Up will not bother for such instances.
     */
    def setDecidable(decidable : Boolean) = {
        this.decidable = decidable;
    }

    /**
     * Returns if the instance is decided or ready to be decided.
     * 
     * FOR CATCH-UP PURPOSES ONLY
     */
    def isDecidable() : Boolean = {
        return LogEntryState.DECIDED.equals(state) || decidable;
    }
    
    /**
     * Returns if the instance is decided or ready to be decided.
     * 
     * FOR CATCH-UP PURPOSES ONLY
     */
    def isDecided() : Boolean = {
        return LogEntryState.DECIDED.equals(state);
    }

    private final var logger : Logger = LoggerFactory.getLogger(classOf[ConsensusInstance]);
}