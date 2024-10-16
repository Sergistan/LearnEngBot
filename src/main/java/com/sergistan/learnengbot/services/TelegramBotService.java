package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.exceptions.ServiceException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBotService extends TelegramLongPollingBot {

    @Autowired
    public TelegramBotService(@Value("${bot_token}") String botToken,
                              YandexTranslateApiService yandexTranslateApiService,
                              RandomEngWordApiService randomEngWordApiService,
                              LearningWordService learningWordService,
                              UserService userService) {
        super(botToken);
        this.yandexTranslateApiService = yandexTranslateApiService;
        this.randomEngWordApiService = randomEngWordApiService;
        this.learningWordService = learningWordService;
        this.userService = userService;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Получение стартового сообщения"));
        listOfCommands.add(new BotCommand("/random", "Получение случайного английского слова с переводом"));
        listOfCommands.add(new BotCommand("/practice", "Вывод сохраненных слов с переводом для повторения"));
        listOfCommands.add(new BotCommand("/help", "Информация как использовать этот бот"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    private final YandexTranslateApiService yandexTranslateApiService;
    private final RandomEngWordApiService randomEngWordApiService;
    private final LearningWordService learningWordService;
    private final UserService userService;

    private static final String START = "/start";
    private static final String RANDOM = "/random";
    private static final String PRACTICE = "/practice";
    private static final String HELP = "/help";
    private static final String NOUN = "noun";
    private static final String VERB = "verb";
    private static final String ADJECTIVE = "adjective";
    private static final String ADVERB = "adverb";
    private static final String LEARN = "learn";
    private static final String MISS = "miss";


    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            MaybeInaccessibleMessage message = update.getCallbackQuery().getMessage();
            CallbackQuery callbackQuery = update.getCallbackQuery();
            switch (data) {
                case NOUN -> {
                    randomCommand(chatId, NOUN);
                    deletedButton(callbackQuery);
                }
                case VERB -> {
                    randomCommand(chatId, VERB);
                    deletedButton(callbackQuery);
                }
                case ADJECTIVE -> {
                    randomCommand(chatId, ADJECTIVE);
                    deletedButton(callbackQuery);
                }
                case ADVERB -> {
                    randomCommand(chatId, ADVERB);
                    deletedButton(callbackQuery);
                }
                case LEARN -> {
                    learningCommand(message);
                    deletedButton(callbackQuery);
                }
                case MISS -> {
                    missingCommand(message);
                    deletedButton(callbackQuery);
                }
            }
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            switch (message) {
                case START -> {
                    String userName = update.getMessage().getChat().getUserName();
                    startCommand(chatId, userName);
                }
                case RANDOM -> choosePartOfSpeech(chatId);
                case PRACTICE -> {
                    String userName = update.getMessage().getChat().getUserName();
                    practice(chatId, userName);
                }
                case HELP -> helpCommand(chatId);
                default -> translateCommand(chatId, message);
            }
        }
    }

    private void deletedButton(CallbackQuery callbackQuery) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(callbackQuery.getMessage().getChatId().toString());
        editMessageReplyMarkup.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessageReplyMarkup.setReplyMarkup(null);
        try {
            execute(editMessageReplyMarkup);
        } catch (Exception e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    private void practice(Long chatId, String username) {
        String words = learningWordService.workoutWords(chatId, username);
        sendMessage(chatId, words);
    }

    public void learningCommand(MaybeInaccessibleMessage message) {
        Long chatId = message.getChatId();
        String userName = ((Message) message).getChat().getUserName();
        String wordWithTranslate = ((Message) message).getText();
        learningWordService.saveWord(chatId, userName, wordWithTranslate);
        String text = """
                Английское слово с переводом: "%s" сохранены для повторения.
                """;
        String formattedText = String.format(text, wordWithTranslate);
        sendMessage(chatId, formattedText);
    }

    public void missingCommand(MaybeInaccessibleMessage message) {
        Long chatId = message.getChatId();
        String userName = ((Message) message).getChat().getUserName();
        String wordWithTranslate = ((Message) message).getText();
        learningWordService.deleteWordIfItWasAddedEarlier(chatId, userName, wordWithTranslate);
        sendMessage(chatId, "Слово удалено из корзины для повторения. Продолжайте искать:)");
    }


    public void startCommand(Long chatId, String userName) {
        String startText = """
                Добро пожаловать в бот для перевода и запоминания английских слов, %s!

                Здесь Вы сможете перевести русское слово на английское.

                Для этого просто напишите слово, которое хотите перевести.

                Дополнительные команды:
                /help - получение справки
                """;
        String formattedText = String.format(startText, userName);
        userService.createUser(chatId, userName);
        sendMessage(chatId, formattedText);
    }

    public void randomCommand(Long chatId, String partOfSpeech) throws URISyntaxException, IOException, InterruptedException {
        String randomWord = randomEngWordApiService.getRandomEnglishWord(partOfSpeech);
        String translatedWord = yandexTranslateApiService.translateEnToRu(randomWord);
        sendMessageFromRandomAndChat(chatId, translatedWord, randomWord);
    }

    public void sendMessageFromRandomAndChat(Long chatId, String translatedWord, String word) {
        String chatIdStr = String.valueOf(chatId);
        String text = """
                %s - %s
                """;
        String formattedText = String.format(text, translatedWord, word);
        SendMessage sendMessage = new SendMessage(chatIdStr, formattedText);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton learnButton = new InlineKeyboardButton();
        learnButton.setText("Запомнить слово");
        learnButton.setCallbackData(LEARN);

        InlineKeyboardButton missButton = new InlineKeyboardButton();
        missButton.setText("Не запоминать слово");
        missButton.setCallbackData(MISS);

        rowInLine.add(learnButton);
        rowInLine.add(missButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    public void choosePartOfSpeech(Long chatId) {
        String chatIdStr = String.valueOf(chatId);
        String text = """
                Выберете нужную часть речи для получения случайного слова:
                """;
        SendMessage sendMessage = new SendMessage(chatIdStr, text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();

        InlineKeyboardButton nounButton = new InlineKeyboardButton();
        nounButton.setText("существительное");
        nounButton.setCallbackData(NOUN);

        InlineKeyboardButton verbButton = new InlineKeyboardButton();
        verbButton.setText("глагол");
        verbButton.setCallbackData(VERB);

        InlineKeyboardButton adjectiveButton = new InlineKeyboardButton();
        adjectiveButton.setText("прилагательное");
        adjectiveButton.setCallbackData(ADJECTIVE);

        InlineKeyboardButton adverbButton = new InlineKeyboardButton();
        adverbButton.setText("наречие");
        adverbButton.setCallbackData(ADVERB);

        rowInLine1.add(nounButton);
        rowInLine1.add(verbButton);
        rowInLine2.add(adjectiveButton);
        rowInLine2.add(adverbButton);

        rowsInLine.add(rowInLine1);
        rowsInLine.add(rowInLine2);

        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    public void translateCommand(Long chatId, String message) throws IOException {
        String translatedWord = yandexTranslateApiService.translateRuToEn(message);
        if (translatedWord.startsWith("Ваше введенное слово не подходит!")) {
            sendMessage(chatId, translatedWord);
        } else {
            sendMessageFromRandomAndChat(chatId, message, translatedWord);
        }
    }

    public void helpCommand(Long chatId) {
        String helpText = """
                Справочная информация по боту
                                
                Для получения случайного английского слова с переводом воспользуйтесь командой: /random
                                
                Для повторения ранее добавленных английских слов с переводом воспользуйтесь командой: /practice
                                
                Для получения перевода русского слова на английский язык просто напишите его в чате.
                                
                Для удаления слова из списка сохраненных слов: напишите это слово снова в чате и нажмите на кнопку "Не запоминать слово"
                """;
        sendMessage(chatId, helpText);
    }

    public void sendMessage(Long chatId, String text) {
        String chatIdStr = String.valueOf(chatId);
        SendMessage sendMessage = new SendMessage(chatIdStr, text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    @Override
    public String getBotUsername() {
        return "LearningEngFromSergistanBot";
    }
}
