// This file imports the Kotlin-generated worklet module using Vite's ?worker&url suffix
// Vite will properly bundle the worklet and resolve all its imports
import workletUrl from '../build/compileSync/js/main/productionExecutable/kotlin/tiny-tiny-engine/HissGeneratorWorkletModule__worklet__module.mjs?worker&url'

// Export the URL for use in Kotlin code
export default workletUrl
