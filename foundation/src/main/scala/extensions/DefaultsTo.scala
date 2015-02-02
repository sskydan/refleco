package extensions

/**
 * FIXME look into type parameter defaults
 *    https://groups.google.com/forum/#!topic/scala-user/u1IUxTYLE8M
 */
sealed class DefaultsTo[A, B]

trait LowPriorityDefaultsTo {
   implicit def overrideDefault[A,B] = new DefaultsTo[A,B]
}

object DefaultsTo extends LowPriorityDefaultsTo {
   implicit def default[B] = new DefaultsTo[B, B]
}
