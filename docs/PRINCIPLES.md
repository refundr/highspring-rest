# Engineering principles

- Favor readability over brevity
- Favor libraries over frameworks
- Avoid the use of any technology that introduces "magic" (an element of surprise) into the software development / debugging process
- Given the choice between build-time code generation or runtime bytecode generation, we favor the former. Code generation creates source code that can be read and debugged, unlike bytecode generation.
    - favour generated code that can be read and debugged over bytecode injected at runtime
- We invest the necessary time to ensure that our software is easy to maintain over the long haul.
- We add tests, refactor, and document our work as we go along, not after the fact.
- Time estimates include this work as an inseparable part of implementing a new feature.
- We work as part of a team. When you write (and document) code, do it with your teammates in mind.
