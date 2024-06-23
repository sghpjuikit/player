# Directory for personas

### Defining persona
Personas are `.txt` files containing system prompt that gives assistant character.
Persona can contain assistant name, role, and contraint or guide assistant's behavior.
It is also possible to define relationships or behavior instructions towards particular speakers,
even forbid speaking with to cerain speakers.

### Persona effect
How the LLM model reacts to particular persona is not deterministic nor guaranteed.
The persona (along other technical prompt definitions) is places as 1st message in the chat history.
It may be necessary to find the right combination of model and persona to achieve desired effect.

### Create or edit persona
Create or edit persona through UI or by simply creatng or modifying new txt file with desired content.

### Changing persona
Change persona through UI or using `llm-chat-sys-prompt=<value> in CLI (here it must be single line)`.