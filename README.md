
 Maiden Framework is a light-weight REST API generator tool for Scala that sits on top of Finch and Quill.

 *THIS IS CURRENTLY VERY MUCH A WORK IN PROGRESS! BETTER EXAMPLES COMING *

 [Quick Start](https://github.com/maiden-ventures/maiden-framework/wiki/Quick-Start)


 ## Highlights
   - Full model and migration generation
   - Automatic generation of model relations
   - API Endpoint generation for get, list, create, update, and delete
   - Optional token-based authentication for all endpoints (oauth2 and  Hawk coming soon!)
   - Circe decoders/encoders for all model types
   - Generation of "magic methods" for models using Quill (example: "User.findByUserName")
   - Automatic parameter validations (eg: max_length, non_empty_string, less_than, etc... ) (See[
 Validations](https://github.com/maiden-ventures/maiden-framework/wiki/Validations)).
 These validations work for Finch `param`s, String, and Numeric types (including `Option`s of those types)
   - Automatic formatting for fields like email, postal_code, social_security, phone (US only)
   - Metrics administration site (via a modified FinchTemplate)
   - Easy deployment via `sbt`
   - Much more!
