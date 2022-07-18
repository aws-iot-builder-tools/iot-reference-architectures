package awslabs.client.ssm;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

public class OrderedBuffer<T extends ItemInSequence> {
    private long nextSequenceNumber = 0L;
    private HashMap<Long, T> internalBuffer = HashMap.empty();

    public OrderedBuffer() {
    }

    public OrderedBuffer<T> addItem(T itemInSequence) {
        synchronized (internalBuffer) {
            if (itemInSequence.getSequenceNumber() < nextSequenceNumber) {
                return this;
            }

            internalBuffer = internalBuffer.put(itemInSequence.getSequenceNumber(), itemInSequence);

            return this;
        }
    }

    public List<T> getNextItems() {
        synchronized (internalBuffer) {
            // Starting from our next sequence number
            List<T> returnValue = Stream.iterate(nextSequenceNumber, value -> value + 1)
                    // Keep getting sequence numbers while they exist in the buffer
                    .takeWhile(internalBuffer::containsKey)
                    // Get the values from the buffer
                    .map(internalBuffer::get)
                    // Unwrap them
                    .map(Option::get)
                    .toList();

            // Remove all of the values we're about to return
            internalBuffer = internalBuffer.removeAll(returnValue.map(ItemInSequence::getSequenceNumber));

            // Update our next sequence number with the max sequence number we just delivered
            nextSequenceNumber = returnValue.maxBy(ItemInSequence::getSequenceNumber)
                    // Get just the sequence number
                    .map(ItemInSequence::getSequenceNumber)
                    // Add one to 1 to get the next sequence number
                    .map(value -> value + 1)
                    // If nothing was sent use the old next sequence number
                    .getOrElse(() -> nextSequenceNumber);

            return returnValue;
        }
    }
}
